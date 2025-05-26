package vpdb.codegen.jaxrs.kt.ext

import org.openapitools.codegen.CodegenOperation
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
          Modifier.isFinal(it.modifiers) -> it.handleFinal(original, this)
          else -> it.handlePlain(original, this)
        }
      }
    if (!needsPartialResponse)
      needsPartialResponse = hasResponseHeaders
  }

  val useJaxRsResponse get() = vendorExtensions?.get("x-return-response") == true
}