package fr.chsfleury.cotton.dsl

import com.uchuhimo.konf.Spec

class EnvironmentDsl(
    private val cottonBuilder: CottonBuilder
) {
    fun specs(vararg specs: Spec) {
        cottonBuilder.specifications.clear()
        cottonBuilder.specifications.addAll(specs)
    }

    fun specs(specs: Collection<Spec>) {
        cottonBuilder.specifications.clear()
        cottonBuilder.specifications.addAll(specs)
    }

    fun sources(vararg sources: String) {
        cottonBuilder.propertySources.clear()
        cottonBuilder.propertySources.addAll(sources)
    }

    fun sources(sources: Collection<String>) {
        cottonBuilder.propertySources.clear()
        cottonBuilder.propertySources.addAll(sources)
    }
}