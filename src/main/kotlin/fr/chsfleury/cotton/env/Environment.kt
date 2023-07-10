package fr.chsfleury.cotton.env

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.ConfigLoaderBuilder

class Environment(
    val resourceFiles: List<String>,
) {
    constructor(vararg resourceFiles: String) : this(resourceFiles.toList())

    val configLoader: ConfigLoader = ConfigLoaderBuilder
        .default()
        .build()

    val config: CottonConfiguration = configLoader.loadConfigOrThrow<CottonConfiguration>(resourceFiles)
    val activeProfiles: List<String> = config.profiles.map(String::trim) + "*"

    inline fun <reified T: Any> loadConfig(): T = configLoader.loadConfigOrThrow<T>(resourceFiles)
    override fun toString(): String {
        return "Environment(activeProfiles=$activeProfiles)"
    }


}