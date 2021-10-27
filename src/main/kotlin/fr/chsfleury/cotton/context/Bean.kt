package fr.chsfleury.cotton.context

import com.google.common.reflect.TypeToken

class Bean<T: Any> (
    val obj: T,
    val type: TypeToken<T>,
    val primary: Boolean,
    val name: String? = null,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Bean<*>

        if (obj != other.obj) return false

        return true
    }

    override fun hashCode(): Int {
        return obj.hashCode()
    }

    override fun toString(): String {
        return "Bean(type=$type, primary=$primary, name=$name)"
    }


}