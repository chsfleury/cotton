package fr.chsfleury.cotton.env.specs

import com.uchuhimo.konf.ConfigSpec

object ServerSpec: ConfigSpec("server") {
    val port by optional(8080)
}