package fr.chsfleury.cotton

import fr.chsfleury.cotton.context.ApplicationContext.Companion.context
import fr.chsfleury.cotton.env.Environment

object ApplicationContextTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val env = Environment("/application.yml")
        val ctx = context(env) {
            singleton<SimpleClass>()
            singleton { "string" }
            singleton { 2 }
            singleton(name = "first") { listOf("hello", "world") }
            singleton(primary = true) { listOf("primary", "world") }
            singleton { listOf(2 , 3) }
            singleton { "key" to 2.4 }

            singleton<Pair<Int, List<String>>>()
        }
        ctx.resolve(1)

        val str: String = ctx.bean()
        println(str)
        val strList: List<String> = ctx.bean()
        println(strList)
        val namedList: List<String> = ctx.bean("first")
        println(namedList)
        val listOfList: List<List<String>> = ctx.bean()
        println(listOfList)
        val setOfList: Set<List<String>> = ctx.bean()
        println(setOfList)
        val keyValue: Pair<String, Double> = ctx.bean()
        println(keyValue)
        val pair: Pair<Int, List<String>> = ctx.bean()
        println(pair)
        val simpleClass: SimpleClass = ctx.bean();
        println(simpleClass)
        val env2: Environment = ctx.bean();
        println(env2)
    }
}