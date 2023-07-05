package fr.chsfleury.cotton.dsl

import fr.chsfleury.cotton.Cotton
import fr.chsfleury.cotton.context.ApplicationContext
import fr.chsfleury.cotton.env.Environment
import fr.chsfleury.cotton.javalin.JavalinStateHolder
import io.javalin.Javalin
import io.javalin.config.JavalinConfig

class CottonBuilder {
    var contextInit: (ApplicationContext.(Environment) -> Unit)? = null
    var javalinInit: (JavalinDsl.() -> Unit)? = null

    var javalinConfigInit: (JavalinConfig.(ApplicationContext, Environment) -> Unit)? = null
    var javalinSetupInit: (Javalin.(ApplicationContext, Environment) -> Unit)? = null
    val propertySources: MutableList<String> = mutableListOf("application.yml")

    fun build(): Cotton {
        val env = Environment(propertySources)

        val context: ApplicationContext = ApplicationContext.context(env, contextInit ?: {})

        val javalinDsl = JavalinDsl(this)
        javalinInit?.invoke(javalinDsl)

        val javalinStateHolder = JavalinStateHolder()
        val javalin = Javalin
            .create { config -> javalinConfigInit?.invoke(config, context, env) }
            .events { e ->
                e.serverStarted { javalinStateHolder.started = true }
            }

        javalinSetupInit?.invoke(javalin, context, env)

        return Cotton(env, context, javalin, javalinStateHolder)
    }
}