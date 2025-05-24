package org.openapitools.codegen

data class UnionImpl(
  val modelPackage: String,
  val imports: Collection<String>,
  val operation: ExtendedOperation,
  val responses: List<ContentPair>,
)

data class ContentPair(
  val response: CodegenResponse,
  val content: ExtendedMediaType,
) {
  @Suppress("unused")
  val hasOneHeader get() = response.responseHeaders?.size == 1

  val contentInfixName get() = content.infixName()
  val contentPrefixName get() = content.prefixName()

  val contentDataType: String? get() = content.schema.dataType

  val contentMimeType: String get() = content.mimeType
}