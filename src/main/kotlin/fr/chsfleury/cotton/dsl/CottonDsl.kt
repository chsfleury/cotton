package fr.chsfleury.cotton.dsl

import fr.chsfleury.cotton.context.ApplicationContext
import fr.chsfleury.cotton.env.Environment

class CottonDsl(
    private val cottonBuilder: CottonBuilder
) {
    fun context(ctxInit: ApplicationContext.(Environment) -> Unit) {
        cottonBuilder.contextInit = ctxInit
    }

    fun javalin(javalinInit: JavalinDsl.() -> Unit) {
        cottonBuilder.javalinInit = javalinInit
    }
}