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


inline val CodegenOperation.inputTypes: Set<String>
  get() = consumes.mapTo(HashSet(consumes.size)) { it["mediaType"] as String }

inline val CodegenOperation.outputTypes: Set<String>
  get() = produces.mapTo(HashSet(produces.size)) { it["mediaType"] as String }

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
inline val OperationsMap.consumes: Set<String>
  get() = (get("consumes") as List<Map<String, String>>).let { maps -> maps.mapTo(HashSet(maps.size)) { it["mediaType"] as String } }


inline val OperationsMap.hasProduces
  get() = get("hasProduces") as Boolean? ?: false

@Suppress("UNCHECKED_CAST")
inline val OperationsMap.produces: Set<String>
  get() = (get("produces") as List<Map<String, String>>).let { maps -> maps.mapTo(HashSet(maps.size)) { it["mediaType"] as String } }

// endregion OperationsMap