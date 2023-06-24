package fr.chsfleury.cotton.context

import com.google.common.reflect.TypeToken
import com.uchuhimo.konf.Config
import fr.chsfleury.cotton.env.Environment
import org.slf4j.LoggerFactory
import java.lang.reflect.Parameter
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType

class ApplicationContext private constructor() {
    private val beans = mutableMapOf<String, MutableSet<Bean<*>>>()
    private var environment: Environment? = null

    /*/////////////////////////////
    ///////// INLINES
    //////////////////////////// */

    inline fun <reified T : Any> singleton(
        name: String? = null,
        primary: Boolean = false,
        profile: String = DEFAULT_PROFILE,
        noinline supplier: ApplicationContext.() -> T
    ) {
        val type = object : TypeToken<T>() {}
        singleton(Bean(supplier(this), type, primary, name), profile)
    }

    inline fun <reified T : Any> singleton(
        name: String? = null,
        primary: Boolean = false,
        profile: String = DEFAULT_PROFILE,
    ) {
        val type: TypeToken<T> = object : TypeToken<T>() {}
        val paramMap = createParamMap(type)
        val defaultConstructor = type.rawType.declaredConstructors[0]
        val params = defaultConstructor.parameters
        val args = params.map { param ->
            bean(getParamTypeToken(param, paramMap), getParamNamedAnnotation(param))
        }.toTypedArray()

        val instance = defaultConstructor.newInstance(*args)
        val bean: Bean<T> = Bean(instance as T, type, primary, name)
        singleton(bean, profile)
    }

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

    inline fun <reified T : Any> bean(name: String? = null): T {
        val type = object : TypeToken<T>() {}
        return bean(type, name)
    }

    inline operator fun <reified T : Any> invoke(name: String? = null): T = bean(name)

    /*/////////////////////////////
    ///////// PUBLIC
    //////////////////////////// */

    fun env(): Environment = environment ?: error("no env set")
    fun env(env: Environment) {
        environment = env
    }

    fun <T : Any> singleton(bean: Bean<T>, profile: String = DEFAULT_PROFILE) {
        registerBean(bean, profile)
    }



    fun <T : Any> bean(type: TypeToken<T>, name: String? = null): T {
        log.debug("trying to get a bean of type '{}' and name '{}'", type, name)
        return when {
            type.rawType.equals(Environment::class.java) -> env() as T
            type.rawType.equals(Config::class.java) -> env() as T
            type.isSubtypeOf(LIST_TYPE) -> {
                val listBean = getOne(type, name)
                if (listBean == null) {
                    val elementType = (type.type as ParameterizedType).actualTypeArguments[0]
                    log.trace("list not found, trying to compose a list with collection of existing '{}' beans", elementType)
                    if (elementType is WildcardType) {
                        getList(TypeToken.of(elementType.upperBounds[0]), name) as T
                    } else {
                        getList(TypeToken.of(elementType), name) as T
                    }
                } else {
                    log.trace("list found, returning it")
                    listBean
                }
            }
            type.isSubtypeOf(SET_TYPE) -> {
                val setBean = getOne(type, name)
                if (setBean == null) {
                    val elementType = (type.type as ParameterizedType).actualTypeArguments[0] as WildcardType
                    getSet(TypeToken.of(elementType.upperBounds[0]), name) as T
                } else {
                    setBean
                }
            }
            else -> getOne(type, name) ?: error("no bean of type $type")
        }
    }

    fun <T : Any> getOne(type: TypeToken<T>, name: String? = null): T? {
        val candidates: Set<Bean<*>> = getCandidates(type, name).toSet()
        val bean = when (candidates.size) {
            0 -> null
            1 -> candidates.iterator().next()
            else -> handlePrimaryBeans(candidates)
        }
        return bean?.obj as? T
    }

    fun <T : Any> getSet(type: TypeToken<T>, name: String? = null): Set<T> = getCandidates(type, name).map { it.obj as T }.toSet()
    fun <T : Any> getList(type: TypeToken<T>, name: String? = null): List<T> = getCandidates(type, name).map { it.obj as T }.toList()

    fun modules(vararg modules: Module) {
        modules.forEach { it.register(this) }
    }

    /*/////////////////////////////
    ///////// PRIVATE
    //////////////////////////// */

    private fun handlePrimaryBeans(candidates: Set<Bean<*>>): Bean<*> {
        val primaryBeans = candidates.filter { it.primary }
        return when (primaryBeans.size) {
            0 -> error("no primary bean found")
            1 -> primaryBeans[0]
            else -> error("more than one primary beans")
        }
    }

    private fun <T : Any> getCandidates(type: TypeToken<T>, name: String?): Sequence<Bean<*>> {
        val env: Environment = environment ?: error("no env set")
        log.trace("get candidates for '{}' and name '{}'", type, name)
        return env.activeProfiles.asSequence()
            .flatMap { getAll(it) }
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

    private fun typeMatch(beanType: TypeToken<*>, candidateType: TypeToken<*>): Boolean {
        return beanType.isSubtypeOf(candidateType)
                || (beanType.rawType.equals(candidateType.rawType) && beanType.isSupertypeOf(candidateType))

    }

    private fun getAll(profile: String): Set<Bean<*>> = beans[profile] ?: emptySet()

    private fun <T : Any> registerBean(bean: Bean<T>, profile: String) {
        log.debug("registering bean {} for profile '{}'", bean, profile)
        beans
            .computeIfAbsent(profile) { mutableSetOf() }
            .add(bean)
    }

    companion object {
        val log = LoggerFactory.getLogger(ApplicationContext::class.java)

        const val DEFAULT_PROFILE = "*"
        val LIST_TYPE: TypeToken<List<*>> = TypeToken.of(List::class.java)
        val SET_TYPE: TypeToken<Set<*>> = TypeToken.of(Set::class.java)

        fun context(env: Environment = Environment(), init: ApplicationContext.(Environment) -> Unit): ApplicationContext = ApplicationContext()
            .apply { env(env) }
            .also{ init(it, env) }
    }
}
