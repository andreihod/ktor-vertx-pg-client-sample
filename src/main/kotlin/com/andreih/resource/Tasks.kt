package com.andreih.resource

import io.ktor.resources.*

@Resource("/tasks")
class Tasks {
    @Resource("{id}")
    class Id(val parent: Tasks = Tasks(), val id: Int) {
        @Resource("done")
        class Done(val parent: Id)
    }
}
