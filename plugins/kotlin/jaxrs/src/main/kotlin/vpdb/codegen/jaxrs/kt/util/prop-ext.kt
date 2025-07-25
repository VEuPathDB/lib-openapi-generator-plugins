package vpdb.codegen.jaxrs.kt.util

import org.openapitools.codegen.CodegenProperty

fun CodegenProperty.cleanTypeNames() {
  ::dataType.cleanSuffix()
  ::datatypeWithEnum.cleanSuffix()
  ::baseType.cleanSuffix()
  if (isMap) {
    ::dataType.cleanMapKey()
    ::datatypeWithEnum.cleanMapKey()
  }
}

