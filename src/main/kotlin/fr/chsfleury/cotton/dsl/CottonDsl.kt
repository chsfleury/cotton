package fr.chsfleury.cotton.dsl

import fr.chsfleury.cotton.Cotton
import fr.chsfleury.cotton.context.ApplicationContext
import fr.chsfleury.cotton.env.Environment
import fr.chsfleury.cotton.javalin.JavalinStateHolder
import io.javalin.Javalin
import io.javalin.config.JavalinConfig

class CottonDsl {
    var contextInit: (ApplicationContext.(Environment) -> Unit)? = null
    var javalinInit: (JavalinDsl.() -> Unit)? = null

    var javalinConfigInit: (JavalinConfig.(ApplicationContext, Environment) -> Unit)? = null
    var javalinSetupInit: (Javalin.(ApplicationContext, Environment) -> Unit)? = null
    var propertySources: List<String> = mutableListOf("/application.yml")

    fun context(ctxInit: ApplicationContext.(Environment) -> Unit) {
        this.contextInit = ctxInit
    }

    fun propertySources(vararg propertySources: String) {
        this.propertySources = propertySources.toList()
    }

    fun javalin(javalinInit: JavalinDsl.() -> Unit) {
        this.javalinInit = javalinInit
    }

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

    companion object {
        fun cotton(cottonInit: CottonDsl.() -> Unit): Cotton = CottonDsl()
            .also(cottonInit)
            .build()
    }
}