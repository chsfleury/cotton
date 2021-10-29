package fr.chsfleury.cotton.dsl

import com.uchuhimo.konf.Spec
import fr.chsfleury.cotton.Cotton
import fr.chsfleury.cotton.context.ApplicationContext
import fr.chsfleury.cotton.env.Environment
import fr.chsfleury.cotton.javalin.JavalinStateHolder
import io.javalin.Javalin
import io.javalin.core.JavalinConfig

class CottonBuilder {
    var environmentInit: (EnvironmentDsl.() -> Unit)? = null
    var contextInit: (ApplicationContext.(Environment) -> Unit)? = null
    var javalinInit: (JavalinDsl.() -> Unit)? = null

    var javalinConfigInit: (JavalinConfig.(ApplicationContext, Environment) -> Unit)? = null
    var javalinSetupInit: (Javalin.(ApplicationContext, Environment) -> Unit)? = null
    val specifications: MutableSet<Spec> = mutableSetOf()
    val propertySources: MutableSet<String> = mutableSetOf("application.yml")

    fun build(): Cotton {
        val envDsl = EnvironmentDsl(this)
        environmentInit?.invoke(envDsl)
        val env = Environment(specifications, propertySources)

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