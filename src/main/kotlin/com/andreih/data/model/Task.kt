package com.andreih.data.model

import arrow.core.Either
import arrow.core.continuations.either
import com.andreih.data.entity.TaskEntity
import com.andreih.data.entity.TaskEntityInsertable
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class TaskId(val value: Int)

@Serializable
@JvmInline
value class TaskDescription(val value: String)

@Serializable
@JvmInline
value class TaskDone(val value: Boolean)

@Serializable
@JvmInline
value class TaskCreatedAt(val value: Instant)

@Serializable
@JvmInline
value class TaskDeletedAt(val value: Instant)

@Serializable
data class Task (
    val id: TaskId,
    val description: TaskDescription,
    val done: TaskDone,
    val createdAt: TaskCreatedAt,
    val deletedAt: TaskDeletedAt?
)

@Serializable
data class TaskCreatable (
    val description: TaskDescription,
)

data class InvalidTaskError(val message: String)

fun TaskEntity.intoTask(): Either<InvalidTaskError, Task> = either.eager {
    ensure(id > 0) { InvalidTaskError("Invalid Id") }
    ensure(description.isNotEmpty()) { InvalidTaskError("Description should be not empty") }

    Task(
        id = TaskId(id),
        description = TaskDescription(description),
        done = TaskDone(done),
        createdAt = TaskCreatedAt(createdAt),
        deletedAt = deletedAt?.let { TaskDeletedAt(it) }
    )
}

fun TaskCreatable.intoTaskEntityInsertable(): Either<InvalidTaskError, TaskEntityInsertable> = either.eager {
    ensure(description.value.isNotEmpty()) { InvalidTaskError("Description should be not empty") }
    TaskEntityInsertable(description.value)
}
