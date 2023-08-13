package fr.chsfleury.cotton.context

import com.google.common.reflect.TypeToken
import fr.chsfleury.cotton.context.ApplicationContextHelper.createParamMap
import fr.chsfleury.cotton.context.ApplicationContextHelper.getParamNamedAnnotation
import fr.chsfleury.cotton.context.ApplicationContextHelper.getParamTypeToken
import fr.chsfleury.cotton.context.ApplicationContextHelper.handlePrimaryBeans
import fr.chsfleury.cotton.env.Environment
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.ParameterizedType
import java.lang.reflect.WildcardType
import java.util.concurrent.atomic.AtomicInteger

class ApplicationContext private constructor() {
    private val beans = mutableSetOf<Bean<*>>()
    private val beanFactories = mutableMapOf<String, MutableList<RegisteredBeanFactory<*>>>()
    private var environment: Environment? = null
    private val beanIndex = AtomicInteger(0)

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
        val beanFactory = LambdaBeanFactory(name, type, primary, supplier)
        registerBeanFactory(profile, beanFactory)
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

        val beanFactory = LambdaBeanFactory(name, type, primary) {
            val args = params.map { param ->
                bean(getParamTypeToken(param, paramMap), getParamNamedAnnotation(param))
            }.toTypedArray()

            defaultConstructor.newInstance(*args) as T
        }
        registerBeanFactory(profile, beanFactory)
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

    fun getAndIncrementIndex(): Int = beanIndex.getAndIncrement()

    fun <T: Any> registerBeanFactory(profile: String, beanFactory: BeanFactory<T>) {
        val registeredBeanFactory = RegisteredBeanFactory(
            beanFactory,
            getAndIncrementIndex()
        )
        beanFactories
            .computeIfAbsent(profile) { mutableListOf() }
            .add(registeredBeanFactory)
    }

    fun <T : Any> bean(type: TypeToken<T>, name: String? = null): T = beanOrNull(type, name) ?: error("no bean of type $type")

    private fun <T : Any> beanOrNull(type: TypeToken<T>, name: String? = null): T? {
        log.debug("trying to get a bean of type '{}' and name '{}'", type, name)
        return when {
            type.rawType.equals(Environment::class.java) -> env() as T
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
            else -> getOne(type, name)
        }
    }

    fun modules(vararg modules: Module) {
        modules.forEach { it.register(this) }
    }

    fun resolve(maxRetry: Int = 3) {
        val env: Environment = environment ?: error("no env set")
        val beanFactories = env.activeProfiles.asSequence()
            .flatMap(this::getAllBeanFactoriesForProfile)
            .sortedBy(RegisteredBeanFactory<*>::beanIndex)
            .toMutableList()

        var retry = -1
        while (beanFactories.isNotEmpty() && retry++ < maxRetry) {
            log.info("Resolving beans ({}/{})", retry + 1, maxRetry + 1)
            val toRemove = mutableListOf<Int>()
            beanFactories.map { beanFactory ->
                val bean = try {
                    beanFactory.create(this)
                } catch (t: Throwable) {
                    log.info("Cannot instantiate bean named '{}' of type '{}' yet", beanFactory.name ?: "no name", beanFactory.type.rawType.name, t)
                    null
                }

                if (bean != null) {
                    beans += bean
                    toRemove += beanFactory.beanIndex
                }
            }

            beanFactories.removeIf { it.beanIndex in toRemove }
            toRemove.clear()
            
            if (beanFactories.isNotEmpty()) {
                log.info("{} bean(s) not resolved yet", beanFactories.size)
            }
        }
    }

    /*/////////////////////////////
    ///////// PRIVATE
    //////////////////////////// */

    private fun <T : Any> getCandidates(type: TypeToken<T>, name: String?): Sequence<Bean<*>> {
        log.trace("get candidates for '{}' and name '{}'", type, name)
        return ApplicationContextHelper.getCandidates(beans, type, name)
    }

    private fun getAllBeanFactoriesForProfile(profile: String): List<RegisteredBeanFactory<*>> = beanFactories[profile] ?: listOf()

    private fun <T : Any> getOne(type: TypeToken<T>, name: String? = null): T? {
        val candidates: Set<Bean<*>> = getCandidates(type, name).toSet()
        val bean = when (candidates.size) {
            0 -> null
            1 -> candidates.iterator().next()
            else -> handlePrimaryBeans(candidates)
        }
        return bean?.obj as? T
    }

    private fun <T : Any> getSet(type: TypeToken<T>, name: String? = null): Set<T> = getCandidates(type, name).map { it.obj as T }.toSet()
    private fun <T : Any> getList(type: TypeToken<T>, name: String? = null): List<T> = getCandidates(type, name).map { it.obj as T }.toList()

    companion object {
        val log: Logger = LoggerFactory.getLogger(ApplicationContext::class.java)

        const val DEFAULT_PROFILE = "*"
        val LIST_TYPE: TypeToken<List<*>> = TypeToken.of(List::class.java)
        val SET_TYPE: TypeToken<Set<*>> = TypeToken.of(Set::class.java)

        fun context(env: Environment, init: ApplicationContext.(Environment) -> Unit): ApplicationContext = ApplicationContext()
            .apply { env(env) }
            .also{ init(it, env) }
    }
}
