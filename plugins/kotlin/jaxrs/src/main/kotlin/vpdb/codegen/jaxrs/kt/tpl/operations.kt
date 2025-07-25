package vpdb.codegen.jaxrs.kt.tpl

import org.openapitools.codegen.CodegenOperation
import org.openapitools.codegen.model.OperationsMap
import vpdb.codegen.jaxrs.kt.SupportPackageKey
import vpdb.codegen.jaxrs.kt.UseJaxRSResponseFlag

// region Operation

inline val CodegenOperation.needsWrapperResponse: Boolean
  get() = responses.size > 1 || responseHeaders.isNotEmpty() || produces.size > 1

inline val CodegenOperation.usesJaxRSResponse: Boolean
  get() = responses.any { UseJaxRSResponseFlag in it.vendorExtensions }


inline val CodegenOperation.inputTypes
  get() = consumes.convertMedaTypes()

inline val CodegenOperation.outputTypes
  get() = produces.convertMedaTypes()

// endregion Operation

// region OperationsMap

inline val OperationsMap.nonModelImports: Set<String>
  get() = imports.asSequence()
    .map { it["import"]!! }
    .filterNot { it.startsWith(modelPackage) }
    .toSet()

inline val OperationsMap.modelImports: Set<String>
  get() = imports.asSequence().map { it["import"]!! }.filter { it.startsWith(modelPackage) }.toSet()


inline val OperationsMap.needsSupportPackage
  get() = containsKey(SupportPackageKey)

inline val OperationsMap.supportPackage
  get() = get(SupportPackageKey) as String


inline val OperationsMap.packageName
  get() = get("package") as String

inline val OperationsMap.className
  get() = get("classname") as String


inline val OperationsMap.modelPackage
  get() = get("modelPackage") as String


inline val OperationsMap.hasConsumes
  get() = get("hasConsumes") as Boolean? ?: false

@Suppress("UNCHECKED_CAST")
inline val OperationsMap.consumes
  get() = (get("consumes") as List<Map<String, String>>).convertMedaTypes()


inline val OperationsMap.hasProduces
  get() = get("hasProduces") as Boolean? ?: false

@Suppress("UNCHECKED_CAST")
inline val OperationsMap.produces
  get() =(get("produces") as List<Map<String, String>>).convertMedaTypes()


fun List<Map<String, String>>.convertMedaTypes() =
  asSequence()
    .map { it["mediaType"] as String }
    .map { when (it) {
      "application/json"            -> "MediaType.APPLICATION_JSON"
      "multipart/form-data"         -> "MediaType.MULTIPART_FORM_DATA"
      "application/octet-stream"    -> "MediaType.APPLICATION_OCTET_STREAM"
      "application/json-patch+json" -> "MediaType.APPLICATION_JSON_PATCH_JSON"
      "application/xml"             -> "MediaType.APPLICATION_XML"
      "text/plain"                  -> "MediaType.TEXT_PLAIN"
      "text/xml"                    -> "MediaType.TEXT_XML"
      "text/html"                   -> "MediaType.TEXT_HTML"
      "*/*"                         -> "MediaType.WILDCARD"
      else                          -> "\"$it\""
    } }
    .toSet()

// endregion OperationsMap