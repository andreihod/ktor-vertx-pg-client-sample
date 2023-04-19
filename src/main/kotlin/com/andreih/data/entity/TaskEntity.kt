package com.andreih.data.entity

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class TaskEntity(
    val id: Int,
    val description: String,
    val done: Boolean,
    val createdAt: Instant,
    val deletedAt: Instant?
)

data class TaskEntityInsertable(
    val description: String
) {
    val createdAt: Instant = Clock.System.now()
}
