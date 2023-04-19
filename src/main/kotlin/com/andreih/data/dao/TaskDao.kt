package com.andreih.data.dao

import arrow.core.Either
import arrow.core.Option
import com.andreih.db.Database
import com.andreih.db.DatabaseError
import com.andreih.data.entity.TaskEntity
import com.andreih.data.entity.TaskEntityInsertable
import io.vertx.sqlclient.Row
import kotlinx.datetime.*
import java.time.ZoneOffset

class TaskDao(private val client: Database) {
    private val taskEntityMapper: (Row) -> TaskEntity = {
        TaskEntity(
            it.getInteger("id"),
            it.getString("description"),
            it.getBoolean("done"),
            it.getInstant("created_at"),
            it.getInstantOrNull("deleted_at"),
        )
    }

    suspend fun insert(task: TaskEntityInsertable): Either<DatabaseError, Unit> {
        return client.insert(
            query = "INSERT INTO tasks (description, created_at) VALUES (#{description}, #{createdAt})",
            mapFrom = {
                mapOf(
                    "description" to it.description,
                    "createdAt" to it.createdAt.toJavaInstant().atOffset(ZoneOffset.UTC)
                )
            },
            param = task
        )
    }

    suspend fun markAsDone(id: Int): Either<DatabaseError, Int> {
        return client.update(
            query = "UPDATE tasks SET done = true WHERE id = #{id}",
            mapFrom = { mapOf("id" to it) },
            param = id
        )
    }

    suspend fun delete(id: Int): Either<DatabaseError, Int> {
        return client.delete(
            query = "UPDATE tasks SET deleted_at = #{deletedAt} WHERE id = #{id}",
            mapFrom = { (id, now) -> mapOf("id" to id, "deletedAt" to now.toJavaInstant().atOffset(ZoneOffset.UTC)) },
            param = Pair(id, Clock.System.now())
        )
    }

    suspend fun find(id: Int): Either<DatabaseError, Option<TaskEntity>> {
        return client.find(
            query = "SELECT id, description, done, created_at, deleted_at FROM tasks WHERE id = #{id}",
            params = mapOf("id" to id),
            mapTo = taskEntityMapper
        )
    }

    suspend fun list(): Either<DatabaseError, List<TaskEntity>> {
        return client.query(
            query = "SELECT id, description, done, created_at, deleted_at FROM tasks ORDER BY created_at",
            mapTo = taskEntityMapper
        )
    }
}

private fun Row.getInstant(column: String): Instant = getOffsetDateTime(column).toInstant().toKotlinInstant()
private fun Row.getInstantOrNull(column: String): Instant? = getOffsetDateTime(column)?.toInstant()?.toKotlinInstant()