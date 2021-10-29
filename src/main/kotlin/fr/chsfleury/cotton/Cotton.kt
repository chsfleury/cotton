package fr.chsfleury.cotton

import fr.chsfleury.cotton.context.ApplicationContext
import fr.chsfleury.cotton.dsl.CottonBuilder
import fr.chsfleury.cotton.dsl.CottonDsl
import fr.chsfleury.cotton.env.Environment
import fr.chsfleury.cotton.env.specs.ServerSpec
import fr.chsfleury.cotton.javalin.JavalinStateHolder
import io.javalin.Javalin

class Cotton(
    val env: Environment,
    val context: ApplicationContext,
    val javalin: Javalin,
    private val javalinStateHolder: JavalinStateHolder
) {
    companion object {
        fun cotton(cottonInit: CottonDsl.() -> Unit): Cotton {
            val cottonBuilder = CottonBuilder()
            val cottonDsl = CottonDsl(cottonBuilder)
            cottonInit(cottonDsl)
            return cottonBuilder.build()
        }
    }

    fun start() {
        if (!javalinStateHolder.started) {
            javalin.start(env[ServerSpec.port])
        }
    }
}