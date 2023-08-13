package fr.chsfleury.cotton.context

data class RegisteredBeanFactory<T: Any> (
    val beanFactory: BeanFactory<T>,
    val beanIndex: Int
): BeanFactory<T> by beanFactory
