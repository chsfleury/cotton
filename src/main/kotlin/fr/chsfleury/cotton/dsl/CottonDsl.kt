package fr.chsfleury.cotton.dsl

import fr.chsfleury.cotton.context.ApplicationContext
import fr.chsfleury.cotton.env.Environment

class CottonDsl(
    private val cottonBuilder: CottonBuilder
) {
    fun context(ctxInit: ApplicationContext.(Environment) -> Unit) {
        cottonBuilder.contextInit = ctxInit
    }

    fun propertySources(vararg propertySources: String) {
        cottonBuilder.propertySources = propertySources.toList()
    }

    fun javalin(javalinInit: JavalinDsl.() -> Unit) {
        cottonBuilder.javalinInit = javalinInit
    }
}