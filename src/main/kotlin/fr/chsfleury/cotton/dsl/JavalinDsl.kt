package fr.chsfleury.cotton.dsl

import fr.chsfleury.cotton.context.ApplicationContext
import fr.chsfleury.cotton.env.Environment
import io.javalin.Javalin
import io.javalin.config.JavalinConfig

class JavalinDsl(
    private val cottonBuilder: CottonBuilder
) {
    fun config(javalinConfigInit: JavalinConfig.(ApplicationContext, Environment) -> Unit) {
        cottonBuilder.javalinConfigInit = javalinConfigInit
    }

    fun setup(javalinInit: Javalin.(ApplicationContext, Environment) -> Unit) {
        cottonBuilder.javalinSetupInit = javalinInit
    }
}