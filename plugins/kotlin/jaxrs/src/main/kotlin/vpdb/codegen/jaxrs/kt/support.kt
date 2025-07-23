package vpdb.codegen.jaxrs.kt

import org.openapitools.codegen.model.ModelsMap

typealias ModelIndex = MutableMap<String, ModelsMap>
typealias EnumDiscriminators = MutableMap<String, Map<Any, String>>

internal val mimeTypeNameOverrides = mapOf(
  "application/json"          to "json",
  "application/octet-stream"  to "binary",
  "application/pdf"           to "pdf",
  "application/xml"           to "xml",
  "application/zip"           to "zip",
  "text/plain"                to "text",
  "text/csv"                  to "csv",
  "text/html"                 to "html",
  "text/tab-separated-values" to "tsv",
  "text/tsv"                  to "tsv",
  "text/xml"                  to "xml",
  "*/*"                       to "any",
)

internal fun Char.isWord() = when (code) { in 97..122, in 48..57, in 65..90, 94 -> true; else -> false }
