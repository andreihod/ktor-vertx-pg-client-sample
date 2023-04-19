package com.andreih.db

import arrow.core.Either
import arrow.core.Option
import arrow.core.continuations.either
import arrow.core.firstOrNone
import io.vertx.kotlin.coroutines.await
import io.vertx.pgclient.PgException
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.templates.SqlTemplate
import io.vertx.sqlclient.templates.TupleMapper
import kotlinx.coroutines.CancellationException
import java.net.SocketException

sealed class DatabaseError(val message: String?) {
    data class Connection(val throwable: SocketException) : DatabaseError(throwable.message)
    data class Execution(val throwable: PgException) : DatabaseError(throwable.message)
    data class Unknown(val throwable: Throwable) : DatabaseError(throwable.message)
    data class MapTo(val throwable: Throwable) : DatabaseError(throwable.message)
    data class MapFrom(val throwable: Throwable) : DatabaseError(throwable.message)
}

class Database(private val pgPool: PgPool) {
    private suspend fun <T> execute(fn: suspend () -> T): Either<DatabaseError, T> =
        Either.catch { fn() }.mapLeft {
            when (it) {
                is SocketException -> DatabaseError.Connection(it)
                is PgException -> DatabaseError.Execution(it)
                is CancellationException -> throw it
                else -> DatabaseError.Unknown(it)
            }
        }

    suspend fun <T> insert(
        query: String,
        mapFrom: (T) -> Map<String, Any>,
        param: T
    ): Either<DatabaseError, Unit> = either.invoke {
        val validMapFrom = Either.catch { mapFrom }.mapLeft(DatabaseError::MapFrom).bind()

        execute {
            SqlTemplate
                .forUpdate(pgPool, query)
                .mapFrom(TupleMapper.mapper(validMapFrom))
                .execute(param)
                .await()
        }.bind()
    }

    suspend fun <T> update(
        query: String,
        mapFrom: (T) -> Map<String, Any>,
        param: T
    ): Either<DatabaseError, Int> = either.invoke {
        val validMapFrom = Either.catch { mapFrom }.mapLeft(DatabaseError::MapFrom).bind()

        execute {
            SqlTemplate
                .forUpdate(pgPool, query)
                .mapFrom(TupleMapper.mapper(validMapFrom))
                .execute(param)
                .await()
                .rowCount()
        }.bind()
    }

    suspend fun <T> delete(
        query: String,
        mapFrom: (T) -> Map<String, Any>,
        param: T
    ): Either<DatabaseError, Int> = update(query, mapFrom, param)

    suspend fun <T> find(
        query: String,
        params: Map<String, Any> = mapOf(),
        mapTo: (Row) -> T
    ): Either<DatabaseError, Option<T>> = either.invoke {
        query(query, params, mapTo).map { it.firstOrNone() }.bind()
    }

    suspend fun <T> query(
        query: String,
        params: Map<String, Any> = mapOf(),
        mapTo: (Row) -> T
    ): Either<DatabaseError, List<T>> = either.invoke {
        val validMapTo = Either.catch { mapTo }.mapLeft(DatabaseError::MapTo).bind()

        execute {
            SqlTemplate
                .forQuery(pgPool, query)
                .mapTo(validMapTo)
                .execute(params)
                .await()
                .toList()
        }.bind()
    }
}