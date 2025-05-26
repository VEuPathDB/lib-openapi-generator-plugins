package vpdb.codegen.jaxrs.kt

import org.openapitools.codegen.CodegenModel
import org.openapitools.codegen.languages.AbstractKotlinCodegen.ADDITIONAL_MODEL_TYPE_ANNOTATIONS
import org.openapitools.codegen.model.ModelMap

class ModelRoot(val map: Map<String, Any>) {
  val packageName get() = map["modelPackage"] as String

  val imports: Set<String> get() = model.imports

  @Suppress("UNCHECKED_CAST")
  val model: CodegenModel get() = (map["models"] as List<ModelMap>)[0].model

  @Suppress("UNCHECKED_CAST")
  val extraTypeAnnotations
    get() = map[ADDITIONAL_MODEL_TYPE_ANNOTATIONS] as List<String>? ?: emptyList()
}
