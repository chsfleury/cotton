package fr.chsfleury.cotton.context

open class Module(val register: ApplicationContext.() -> Unit) {
  companion object {
    fun module(init: ApplicationContext.() -> Unit) = Module(init)
  }
}
