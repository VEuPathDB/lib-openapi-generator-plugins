package vpdb.codegen.jaxrs.kt.tpl

private val DollarRegex = Regex("(?<!\\\\)\\\$")

internal fun String.escapeDollar(): String = replace(DollarRegex, "\\\\\\\$")
