package org.openapitools.codegen

import java.lang.reflect.Field
import java.lang.reflect.Modifier

@Suppress("MemberVisibilityCanBePrivate")
class ExtendedOperation(original: CodegenOperation): CodegenOperation() {
  var extraImports = HashSet<String>(6)
  var hasMultipleResponses = original.responses.size > 1
  var needsPartialResponse = hasMultipleResponses

  init {
    CodegenOperation::class.java.fields
      .asSequence()
      .filter { Modifier.isPublic(it.modifiers) }
      .filterNot { Modifier.isStatic(it.modifiers) }
      .onEach { it.isAccessible = true }
      .forEach {
        when {
          Modifier.isFinal(it.modifiers) -> it.handleFinal(original)
          else -> it.handlePlain(original)
        }
      }
    if (!needsPartialResponse)
      needsPartialResponse = hasResponseHeaders
  }

  @Suppress("UNCHECKED_CAST")
  private fun Field.handleFinal(og: CodegenOperation) {
    when {
      MutableCollection::class.java.isAssignableFrom(type) -> {
        val col = get(this@ExtendedOperation) as MutableCollection<Any>
        col.clear()
        col.addAll(get(og) as MutableCollection<Any>)
      }

      MutableMap::class.java.isAssignableFrom(type)        -> {
        val map = get(this@ExtendedOperation) as MutableMap<Any, Any>
        map.clear()
        map.putAll(get(og) as Map<out Any, Any>)
      }
    }
  }

  private fun Field.handlePlain(og: CodegenOperation) = set(this@ExtendedOperation, get(og))
}