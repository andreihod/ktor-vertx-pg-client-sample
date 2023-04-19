package com.andreih.repository

import arrow.core.*
import arrow.core.continuations.either
import com.andreih.data.dao.TaskDao
import com.andreih.data.entity.TaskEntity
import com.andreih.data.model.*
import com.andreih.db.DatabaseError

sealed class TaskRepositoryError(val message: String) {
    data class Database(val databaseError: DatabaseError): TaskRepositoryError(databaseError.message ?: "Database error")
    data class Validation(val invalidTaskError: InvalidTaskError): TaskRepositoryError(invalidTaskError.message)
}

class TaskRepository(private val taskDao: TaskDao) {
    suspend fun create(creatable: TaskCreatable): Either<TaskRepositoryError, Unit> = either.invoke {
        val insertable = creatable
            .intoTaskEntityInsertable()
            .mapLeft(TaskRepositoryError::Validation)
            .bind()

        taskDao.insert(insertable).mapLeft(TaskRepositoryError::Database).bind()
    }

    suspend fun delete(id: TaskId): Either<TaskRepositoryError, Int> = either.invoke {
        taskDao.delete(id.value).mapLeft(TaskRepositoryError::Database).bind()
    }

    suspend fun markAsDone(id: TaskId): Either<TaskRepositoryError, Int> = either.invoke {
        taskDao.markAsDone(id.value).mapLeft(TaskRepositoryError::Database).bind()
    }

    suspend fun find(id: TaskId): Either<TaskRepositoryError, Option<Task>> = either.invoke {
        taskDao
            .find(id.value)
            .mapLeft(TaskRepositoryError::Database)
            .bind()
            .map(TaskEntity::intoTask)
            .sequence()
            .mapLeft(TaskRepositoryError::Validation)
            .bind()
    }

    suspend fun list(): Either<TaskRepositoryError, List<Task>> = either.invoke {
        taskDao
            .list()
            .mapLeft(TaskRepositoryError::Database)
            .bind()
            .traverse(TaskEntity::intoTask)
            .mapLeft(TaskRepositoryError::Validation)
            .bind()
    }
}