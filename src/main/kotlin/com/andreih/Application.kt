package com.andreih

import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.fx.coroutines.ExitCase
import arrow.fx.coroutines.ResourceScope
import arrow.fx.coroutines.continuations.ResourceDSL
import arrow.fx.coroutines.resourceScope
import com.andreih.data.dao.TaskDao
import com.andreih.data.model.TaskCreatable
import com.andreih.data.model.TaskId
import com.andreih.repository.TaskRepository
import com.andreih.db.Database
import com.andreih.resource.Tasks
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.await
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

fun main(): Unit = SuspendApp {
    val env = Env()
    val vertx = Vertx.vertx()

    resourceScope {
        val pgOptions = PgConnectOptions()
            .setPort(env.dataSource.port)
            .setHost(env.dataSource.host)
            .setUser(env.dataSource.user)
            .setPassword(env.dataSource.password)
            .setDatabase(env.dataSource.database)

        val database = Database(
            closeableSqlClient {
                PgPool.pool(
                    vertx,
                    pgOptions,
                    PoolOptions().setMaxSize(16)
                )
            }
        )

        val taskRepository = TaskRepository(TaskDao(database))

        server(Netty, port = 8080) {
            install(Resources)
            install(CallLogging)
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }

            routing {
                get<Tasks> {
                    taskRepository
                        .list()
                        .onLeft { call.respond(HttpStatusCode.InternalServerError, it.message) }
                        .onRight { call.respond(it) }
                }

                get<Tasks.Id> { resource ->
                    taskRepository
                        .find(TaskId(resource.id))
                        .onLeft { call.respond(HttpStatusCode.InternalServerError, it.message) }
                        .onRight { call.respond(it) }
                }

                post<Tasks> {
                    taskRepository
                        .create(call.receive<TaskCreatable>())
                        .onLeft { call.respond(HttpStatusCode.InternalServerError, it.message) }
                        .onRight { call.respond(it) }
                }
            }
        }

        awaitCancellation()
    }
}

@ResourceDSL
suspend fun <A : SqlClient> ResourceScope.closeableSqlClient(
    closingDispatcher: CoroutineDispatcher = kotlinx.coroutines.Dispatchers.IO,
    closeable: suspend () -> A,
): A = install({ closeable() }) { s: A, _: ExitCase ->
    withContext(closingDispatcher) {
        s.close().await()
    }
}
