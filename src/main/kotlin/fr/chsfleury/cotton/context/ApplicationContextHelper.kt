package fr.chsfleury.cotton.context

import com.google.common.reflect.TypeToken
import fr.chsfleury.cotton.env.Environment
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.Parameter
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable

object ApplicationContextHelper {
    val log: Logger = LoggerFactory.getLogger(ApplicationContextHelper::class.java)

    fun getParamTypeToken(param: Parameter, paramMap: Map<TypeVariable<*>, Type>?): TypeToken<*> {
        if (paramMap != null) {
            val parameterizedType = param.parameterizedType;
            if (parameterizedType is TypeVariable<*>) {
                val targetType = paramMap[parameterizedType]
                if (targetType != null) {
                    return TypeToken.of(targetType)
                }
            }
        }
        return TypeToken.of(param.parameterizedType)
    }

    fun getParamNamedAnnotation(param: Parameter): String? {
        return if (param.declaredAnnotations != null) {
            param.declaredAnnotations
                .find { a -> a is Named }
                ?.let { named -> (named as Named).value }
        } else {
            null
        }
    }

    inline fun <reified T : Any> createParamMap(typeToken: TypeToken<T>): Map<TypeVariable<*>, Type>? {
        val type = typeToken.type
        return if (type is ParameterizedType) {
            val map = mutableMapOf<TypeVariable<*>, Type>()
            (typeToken.rawType as Class<*>).typeParameters.forEachIndexed { i, typeVariable ->
                map[typeVariable] = type.actualTypeArguments[i]
            }
            map
        } else {
            null
        }
    }

    internal fun handlePrimaryBeans(candidates: Set<Bean<*>>): Bean<*> {
        val primaryBeans = candidates.filter { it.primary }
        return when (primaryBeans.size) {
            0 -> error("no primary bean found")
            1 -> primaryBeans[0]
            else -> error("more than one primary beans")
        }
    }

    internal fun typeMatch(beanType: TypeToken<*>, candidateType: TypeToken<*>): Boolean {
        return beanType.isSubtypeOf(candidateType)
                || (beanType.rawType.equals(candidateType.rawType) && beanType.isSupertypeOf(candidateType))
    }

    internal fun <T : Any> getCandidates(beans: Set<Bean<*>>, type: TypeToken<T>, name: String?): Sequence<Bean<*>> {
        log.trace("get candidates for '{}' and name '{}'", type, name)
        return beans.asSequence()
            .filter { bean ->
                log.trace(" - {}:", bean)
                if (typeMatch(bean.type, type)) {
                    if (name != null) {
                        if (name == bean.name) {
                            log.trace("    type match and name is OK")
                            true
                        } else {
                            log.trace("    type match and but different name or no name")
                            false
                        }
                    } else {
                        log.trace("    type match")
                        true
                    }
                } else {
                    log.trace("    type does not match")
                    false
                }
            }
    }
}