package fr.chsfleury.cotton.context

import com.google.common.reflect.TypeToken

interface BeanFactory<T: Any> {
    val name: String?
    val type: TypeToken<T>
    val primary: Boolean

    fun create(context: ApplicationContext): Bean<T>
}