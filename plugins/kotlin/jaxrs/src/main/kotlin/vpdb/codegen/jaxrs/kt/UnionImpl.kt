package vpdb.codegen.jaxrs.kt

import org.openapitools.codegen.CodegenParameter
import org.openapitools.codegen.CodegenResponse

data class UnionImpl(
  val packageName: String,
  val imports: Collection<String>,
  val operation: ExtendedOperation,
  val responses: List<ContentPair>,
)

data class ContentPair(
  val response: CodegenResponse,
  val content: ExtendedMediaType,
) {
  val headers: List<CodegenParameter> get() = response.responseHeaders

  val contentPrefixName get() = content.prefixName()

  val contentDataType: String? get() = content.schema.dataType

  val contentMimeType: String get() = content.mimeType
}