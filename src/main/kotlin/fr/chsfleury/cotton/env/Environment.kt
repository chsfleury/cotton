package fr.chsfleury.cotton.env

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.Spec
import com.uchuhimo.konf.source.yaml
import fr.chsfleury.cotton.env.specs.AppSpec
import fr.chsfleury.cotton.env.specs.ServerSpec

open class Environment(
    specs: MutableSet<Spec> = mutableSetOf(),
    resourceFiles: Set<String> = setOf("application.yml"),
    private val env: Config = Config { specs.apply { addAll(builtins) }.forEach { addSpec(it) } }
        .let {
            var current = it
            for(rc in resourceFiles) {
                current = current.from.yaml.resource(rc)
            }
            current
        }
        .from.env()
        .from.systemProperties()
) : Config by env {
    val activeProfiles: List<String> = env[AppSpec.profiles]
        .splitToSequence(',')
        .map { it.trim() }
        .toList() + "*"

    companion object {
        val builtins = setOf(AppSpec, ServerSpec)
    }
}