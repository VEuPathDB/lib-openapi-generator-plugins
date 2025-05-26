package vpdb.codegen.jaxrs.kt

import org.openapitools.codegen.CodegenOperation
import org.openapitools.codegen.CodegenParameter
import org.openapitools.codegen.CodegenResponse
import vpdb.codegen.jaxrs.kt.ext.ExtendedMediaType

data class UnionImpl(
  val packageName: String,
  val modelPackage: String,
  val operation: CodegenOperation,
  val responses: List<ContentPair>,
) {
  val imports: Set<String> =
    HashSet<String>(operation.responseHeaders.size + operation.responses.size).apply {
      operation.responseHeaders.forEach {
        if (it.baseType != null) {
          if (!it.baseType.contains('.')) {
            if (it.baseType !in kotlinBuiltIns) {
              add("$modelPackage.${it.baseType}")
            }
          } else {
            add(it.baseType)
          }
        }
      }
      operation.responses.forEach {
        if (it.baseType != null) {
          if (!it.baseType.contains('.')) {
            if (it.baseType !in kotlinBuiltIns) {
              add("$modelPackage.${it.baseType}")
            }
          } else {
            add(it.baseType)
          }
        }
      }
    }

  inline val nonModelImports
    get() = imports.filterNot { it.startsWith(modelPackage) }

  inline val modelImports
    get() = imports.filter { it.startsWith(modelPackage) }

  inline val useStarModelImport
    get() = imports.count { it.startsWith(modelPackage) } > 4
}

data class ContentPair(
  val response: CodegenResponse,
  val content: ExtendedMediaType,
) {
  val headers: List<CodegenParameter> get() = response.responseHeaders

  val contentPrefixName get() = content.prefixName()

  val contentDataType: String? get() = content.schema.dataType

  val contentMimeType: String get() = content.mimeType
}