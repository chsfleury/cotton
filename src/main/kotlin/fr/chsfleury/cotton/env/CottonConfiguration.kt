package fr.chsfleury.cotton.env

import fr.chsfleury.cotton.env.properties.ServerProperties

data class CottonConfiguration(
    val profiles: List<String> = listOf("DEV"),
    val server: ServerProperties = ServerProperties()
)