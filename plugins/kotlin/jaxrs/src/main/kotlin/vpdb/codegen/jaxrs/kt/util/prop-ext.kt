package vpdb.codegen.jaxrs.kt.util

import org.openapitools.codegen.CodegenProperty

fun CodegenProperty.cleanTypeNames() {
  cleanSuffix(::dataType, ::dataType::set)
  cleanSuffix(::baseType, ::baseType::set)
}