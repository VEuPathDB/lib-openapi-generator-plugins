package vpdb.codegen.jaxrs.kt.util

import kotlin.reflect.KMutableProperty0

private val suffixPattern = Regex("_*(\\d+|allOf|oneOf|anyOf)(?=\\b|$)")

fun KMutableProperty0<String?>.cleanSuffix() {
  get()?.replace(suffixPattern, "")?.let(::set)
}

fun String.hasTypeSuffix() = contains(suffixPattern)

private val mapKeyPattern = Regex("\\bkotlin\\.(\\w+)\\b")

fun KMutableProperty0<String?>.cleanMapKey() {
  get()?.takeIf { it.contains(mapKeyPattern) }
    ?.let { og -> og.replace(mapKeyPattern) { it.groupValues[1] } }
    ?.let(::set)
}
