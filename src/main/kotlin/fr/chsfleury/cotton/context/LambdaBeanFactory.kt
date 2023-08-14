package fr.chsfleury.cotton.context

import com.google.common.reflect.TypeToken

class LambdaBeanFactory<T: Any>(
    override val name: String?,
    override val type: TypeToken<T>,
    override val primary: Boolean,
    private val supplier: ApplicationContext.() -> T
): BeanFactory<T> {
    override fun create(context: ApplicationContext): Bean<T> = Bean(
        supplier(context),
        type,
        primary,
        name
    )

    override fun toString(): String = "LambdaBeanFactory { name: $name, type: ${type.rawType.name}, primary: $primary }"
}