package fr.chsfleury.cotton.env.specs

import com.uchuhimo.konf.ConfigSpec

object AppSpec: ConfigSpec("app") {

    val profiles by optional("DEV")

}