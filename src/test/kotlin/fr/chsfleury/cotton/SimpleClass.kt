package fr.chsfleury.cotton

import fr.chsfleury.cotton.context.Named

data class SimpleClass(
    val str: String,
    val n: List<Int>,
    @Named("first") val strings: List<String>
)