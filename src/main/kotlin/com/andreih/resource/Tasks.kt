package com.andreih.resource

import io.ktor.resources.*

@Resource("/tasks")
class Tasks {
    @Resource("{id}")
    class Id(val id: Int)
}