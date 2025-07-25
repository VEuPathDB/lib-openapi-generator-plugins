package vpdb.codegen.jaxrs.kt.util

import org.openapitools.codegen.CodegenParameter

private val mapKeyPattern = Regex("\\bkotlin\\.(\\w+)\\b")

fun CodegenParameter.fixMapKey() {
  if (dataType.contains(mapKeyPattern)) {
    ::dataType.cleanMapKey()
    ::datatypeWithEnum.cleanMapKey()
  }
}