package fr.chsfleury.cotton.context

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention
annotation class Named(
    val value: String
)
