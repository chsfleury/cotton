package fr.chsfleury.cotton.controller

import io.javalin.apibuilder.ApiBuilder.path

abstract class AbstractController(
    val path: String?,
    val init: () -> Unit
): Controller {
    constructor(init: () -> Unit) : this(null, init)

    override fun setup() {
        if (path != null) {
            path(path) {
                init()
            }
        } else {
            init()
        }
    }
}