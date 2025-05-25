package vpdb.codegen.jaxrs.kt

import io.swagger.v3.core.util.Json
import org.openapitools.codegen.*
import org.openapitools.codegen.languages.KotlinServerCodegen
import org.openapitools.codegen.model.ModelMap
import org.openapitools.codegen.model.ModelsMap
import org.openapitools.codegen.model.OperationMap
import org.openapitools.codegen.model.OperationsMap
import org.openapitools.codegen.utils.ModelUtils
import java.io.File
import java.nio.file.Path
import java.util.IdentityHashMap
import kotlin.io.path.Path
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createDirectories

class VPDBJaxRSKotlinGenerator: KotlinServerCodegen(), CodegenConfig {
  private var generateSupportTypes = false

  private var debugModels: Path? = null
  private var debugOperations: Path? = null

  private var operations: OperationsMap? = null

  init {
    languageSpecificPrimitives = HashSet<String>(languageSpecificPrimitives.size + 4).apply {
      languageSpecificPrimitives.mapTo(this) { it.substringAfterLast('.') }
      add("UByte")
      add("UShort")
      add("UInt")
      add("ULong")
    }
    defaultIncludes = HashSet<String>(defaultIncludes.size + 4).apply {
      defaultIncludes.mapTo(this) { it.substringAfterLast('.') }
    }
    typeMapping.replaceAll { _, value -> value.substringAfterLast('.') }
    instantiationTypes.replaceAll { _, value -> value.substringAfterLast('.') }
    templateDir = null
    embeddedTemplateDir = null

    modelTemplateFiles.clear()
    modelTemplateFiles["model/root.kte"] = ".kt"

    apiTemplateFiles.clear()
    apiTemplateFiles["api/api.kte"] = ".kt"
  }

  override fun getTemplatingEngine() = JteAdaptor

  override fun getName() = "vpdb"

  override fun processOpts() {
    super.processOpts()
    setLibrary(Constants.JAXRS_SPEC)

    if ("debugModels" in additionalProperties)
      when (val path = additionalProperties["debugModels"]) {
        is String -> debugModels = Path(path)
        is File   -> debugModels = path.toPath()
        is Path   -> debugModels = path
      }

    if ("debugOperations" in additionalProperties)
      when (val path = additionalProperties["debugOperations"]) {
        is String -> debugOperations = Path(path)
        is File   -> debugOperations = path.toPath()
        is Path   -> debugOperations = path
      }

    additionalProperties[Constants.INTERFACE_ONLY] = true
    additionalProperties[Constants.OMIT_GRADLE_WRAPPER] = true
  }

  // region Model Processing

  override fun postProcessResponseWithProperty(response: CodegenResponse, property: CodegenProperty?) {
    super.postProcessResponseWithProperty(response, property)
    response.content?.replaceAll(ExtendedMediaType.Companion::extend)
  }

  override fun postProcessAllModels(models: Map<String, ModelsMap>) =
    (super.postProcessAllModels(models) as Map<String, ModelsMap>)
      .also { out ->
        val enumDiscriminators = HashMap<String, Map<Any, String>>()

        out.asSequence()
          .onEach { (_, map) -> map.imports = emptyList() }
          .map { it.key }
          .mapNotNull { ModelUtils.getModelByName(it, models) }
          .onEach { it.imports = it.imports.filterTo(HashSet(it.imports.size)) { import -> import.contains('.') } }
          .onEach { it.processUnsigned() }
          .filterNot { it.parentModel?.discriminator == null }
          .filterNot { it.parentModel.discriminator.propertyType in languageSpecificPrimitives }
          .forEach { it.processDiscriminator(models, enumDiscriminators) }
      }
      .also { out -> debugModels?.bufferedWriter()?.use { Json.pretty().writeValue(it, out) } }

  private fun CodegenModel.processUnsigned() {
    arrayOf(vars, allVars, requiredVars, optionalVars, readOnlyVars, readWriteVars)
      .processProperties {
        when {
          it.isLong    -> it.overrideDataType("ULong")
          it.isInteger -> it.overrideDataType("UInt")
        }
        BreakState.Continue
      }
  }

  private fun CodegenModel.processDiscriminator(
    models: Map<String, ModelsMap>,
    enumDiscriminators: MutableMap<String, Map<Any, String>>,
  ) {
    val disc = ModelUtils.getModelByName(parentModel.discriminator.propertyType, models)
      ?: return

    if (disc.isEnum)
      processEnumDiscriminator(disc, enumDiscriminators)
    else
      processRawDiscriminator()
  }

  private fun CodegenModel.processEnumDiscriminator(
    disc: CodegenModel,
    enumDiscriminators: MutableMap<String, Map<Any, String>>,
  ) {
    val discField = parentModel.discriminator.propertyBaseName
    val discValue = vendorExtensions["x-discriminator-value"] as String

    arrayOf(vars, allVars, requiredVars, optionalVars, readOnlyVars, readWriteVars)
      .processProperties {
        if (it.defaultValue == null && it.baseName == discField) {
          it.defaultValue = enumDiscriminators.getOrComputeDefaultValue(discValue, disc)
          it.isReadOnly = true

          BreakState.SkipCollection
        } else {
          BreakState.Continue
        }
      }

    vendorExtensions["x-has-data-class-body"] = true
  }

  private fun CodegenModel.processRawDiscriminator() {
    if (defaultValue == null)
      defaultValue = vendorExtensions["x-discriminator-value"].toString()
  }

  // endregion Model Processing

  // region Operation Processing

  override fun postProcessOperationsWithModels(allOps: OperationsMap, allModels: MutableList<ModelMap>) =
    (super.postProcessOperationsWithModels(allOps, allModels) as OperationsMap)
      .apply(::extend)
      .also { op -> op.operations.extendedOperation.forEach {
        if (it.hasMultipleResponses || it.hasResponseHeaders)
          generateSupportTypes = true
      } }
      .apply {
        imports = imports
          .filterNotTo(ArrayList(8)) { it["import"]?.startsWith(modelPackage) == true }
          .also { it.add(mapOf("import" to "$modelPackage.*")) }

        val extraImports = HashSet<String>(4)

        operations.extendedOperation.forEach {
          if (it.needsPartialResponse)
            extraImports.add("$modelPackage.support.*")
          if (it.useJaxRsResponse)
            extraImports.add("jakarta.ws.core.Response")
          if (it.allParams.any { p -> p.isFormParam && !p.isFile })
            extraImports.add("java.io.InputStream")
        }

        extraImports.sorted().forEach { imports.add(mapOf("import" to it)) }
      }
      .apply {
        val newImports = HashSet<String>()
        operations.extendedOperation.forEach { op ->
          op.allParams.forEach { it.fixStdLibImports(newImports) }
        }
        vendorExtensions["extraImports"] = newImports
      }
      .also { operations = it }
      .also { ops -> debugOperations?.bufferedWriter()?.use { Json.pretty().writeValue(it, ops) } }

  private fun CodegenParameter.fixStdLibImports(newImports: MutableSet<String>) {
    when {
      dataType == "java.io.File" -> {
        newImports.add("java.io.File")
        overrideDataType("File")
      }

      isArray -> {
        when (items.dataType) {
          "java.io.File" -> "File"
          else           -> null
        }?.also {
          newImports.add(items.dataType)
          overrideDataType(dataType.replace(items.dataType, it))
          items.overrideDataType("File")
        }
      }
    }
  }

  private fun extend(ops: OperationsMap) {
    ops.operations.operation = ops.operations.operation.map(::ExtendedOperation)
  }

  @Suppress("UNCHECKED_CAST")
  private val OperationMap.extendedOperation
    get() = operation as List<ExtendedOperation>

  // endregion Operation Processing

  override fun postProcess() {
    if (generateSupportTypes) {
      val supportPackage = "$modelPackage.support"
      val supportFolder  = Path(modelFileFolder(), "support")

      supportFolder.createDirectories()

      JteAdaptor.compileTemplate(supportPackage, "model/union/def.kte", supportFolder.resolve("UnionResponse.kt"))

      operations!!.operations.operation.asSequence()
        .map {
          (it as ExtendedOperation).let { op ->
            UnionImpl(
              supportPackage,
              op.extraImports.also { it.add("$modelPackage.*") },
              op,
              op.responses.asSequence()
                .flatMap { res -> res.content?.asSequence()?.map { con -> res to con } ?: emptySequence() }
                .map { (res, pair) -> ContentPair(res, ExtendedMediaType.extend(pair.key, pair.value)) }
                .toList()
            )
          }
        }.forEach {
          JteAdaptor.compileTemplate(it, "model/union/impl.kte", supportFolder.resolve("Union${it.operation.operationIdCamelCase}Response.kt"))
        }
    }
  }

  // region Utilities

  private enum class BreakState { Continue, SkipCollection, Break }

  private fun Array<List<CodegenProperty>>.processProperties(fn: (CodegenProperty) -> BreakState) {
    val instances = IdentityHashMap<CodegenProperty, Unit>()

    for (it in this) {
      when (it.processProperties(instances, fn)) {
        BreakState.Break -> return
        else             -> continue
      }
    }
  }

  private fun Iterable<CodegenProperty>.processProperties(
    instances: IdentityHashMap<CodegenProperty, Unit> = IdentityHashMap(),
    fn: (CodegenProperty) -> BreakState,
  ): BreakState {
    for (prop in this) {
      if (prop in instances)
        continue

      when (val state = fn(prop)) {
        BreakState.Continue -> continue
        BreakState.SkipCollection,
        BreakState.Break,
          -> return state
      }
    }

    return BreakState.Continue
  }

  private fun CodegenParameter.overrideDataType(type: String) {
    dataType = type
    baseType = type
    datatypeWithEnum = type
  }

  private fun CodegenProperty.overrideDataType(type: String) {
    dataType = type
    datatypeWithEnum = type
    baseType = type
  }

  @Suppress("UNCHECKED_CAST")
  private fun MutableMap<String, Map<Any, String>>.getOrComputeDefaultValue(
    name: String,
    disc: CodegenModel,
  ): String {
    get(disc.classname)?.also { return it[name]!! }

    val enumVals = disc.allowableValues["enumVars"] as List<Map<String, Any>>
    val rawVals = disc.allowableValues["values"] as List<Any>

    return enumVals.asSequence()
      .mapIndexed { i, it -> rawVals[i] to "${disc.classname}.${it["name"]}" }
      .toMap()
      .also { put(disc.classname, it) }[name]!!
  }

  // endregion Utilities
}
