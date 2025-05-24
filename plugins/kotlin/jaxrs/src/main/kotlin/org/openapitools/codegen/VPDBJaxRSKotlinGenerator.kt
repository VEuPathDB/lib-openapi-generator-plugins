package org.openapitools.codegen

import com.samskivert.mustache.Template.Fragment
import io.swagger.v3.core.util.Json
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.WordUtils
import org.openapitools.codegen.api.TemplatePathLocator
import org.openapitools.codegen.languages.KotlinServerCodegen
import org.openapitools.codegen.model.ModelMap
import org.openapitools.codegen.model.ModelsMap
import org.openapitools.codegen.model.OperationMap
import org.openapitools.codegen.model.OperationsMap
import org.openapitools.codegen.templating.MustacheEngineAdapter
import org.openapitools.codegen.templating.TemplateManagerOptions
import org.openapitools.codegen.utils.ModelUtils
import java.io.File
import java.io.Writer
import java.nio.file.Path
import java.util.IdentityHashMap
import kotlin.io.path.Path
import kotlin.io.path.bufferedWriter

class VPDBJaxRSKotlinGenerator: KotlinServerCodegen(), CodegenConfig {

  companion object {
    private const val DefaultPackage = "org.veupathdb.service.example"
  }

  private var generateSupportTypes = false

  private var debugModels: Path? = null
  private var debugOperations: Path? = null

  private var operations: OperationsMap? = null

  lateinit var supportPackage: String
    private set

  override fun getName() = "vpdb-jaxrs-kt"

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
    embeddedTemplateDir = "vpdb-jaxrs-kt"
    templateDir = embeddedTemplateDir

    addMustacheLambdas().put("indent", ::indent)
    addMustacheLambdas().put("reindent2", ::reindent2)
    addMustacheLambdas().put("reindent4", ::reindent4)
  }

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

    (additionalProperties["basePackage"] as String? ?: DefaultPackage).also {
      apiPackage     = apiPackage ?: "$it.api"
      modelPackage   = modelPackage ?: "$it.model"
      supportPackage = additionalProperties["supportPackage"] as String? ?: "$it.support"
    }
  }

  // region Mustache Lambdas

  private fun indent(frag: Fragment, out: Writer) {
    frag.execute()
      .takeUnless { it.isNullOrEmpty() }
      ?.also { content ->
        val buffer = out.buffered()

        content.lineSequence()
          .forEach { buffer.append("  ").append(it).appendLine() }

        buffer.flush()
      }
  }

  private fun reindent2(frag: Fragment, out: Writer) {
    reindentN(frag, out, "  ")
  }

  private fun reindent4(frag: Fragment, out: Writer) {
    reindentN(frag, out, "    ")
  }

  private fun reindentN(frag: Fragment, out: Writer, indent: String) {
    frag.execute()
      .takeUnless { it.isNullOrEmpty() }
      ?.also { content ->
        out.buffered().apply {
          var trim = -1
          content.lineSequence()
            .forEach {
              if (trim == -1)
                trim = it.indexOfNot(' ')

              if (it.isNotBlank())
                append(indent).append(it, trim, it.length).appendLine()
            }
        }.flush()
      }
  }

  private fun String.indexOfNot(char: Char): Int {
    for (i in indices)
      if (get(i) != char)
        return i

    return -1
  }

  // endregion Mustache Lambdas

  // region Model Processing

  override fun postProcessResponseWithProperty(response: CodegenResponse, property: CodegenProperty?) {
    super.postProcessResponseWithProperty(response, property)
    response.content?.replaceAll(ExtendedMediaType::extend)
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
    enumDiscriminators: MutableMap<String, Map<Any, String>>
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
    enumDiscriminators: MutableMap<String, Map<Any, String>>
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
      val tplMan = TemplateManager(TemplateManagerOptions(isEnableMinimalUpdate, isSkipOverwrite), templatingEngine, arrayOf(
        TemplatePathLocator { "$templateDir/$it".takeIf { javaClass.classLoader.getResource(it) != null } }
      ))

      tplMan.write(
        mapOf("modelPackage" to modelPackage),
        "union/def.mustache",
        File(modelFileFolder() + File.separator + "UnionResponse.kt")
      )

      operations!!.operations.operation.asSequence()
        .map {
          (it as ExtendedOperation).let { op ->
            UnionImpl(
              modelPackage,
              op.extraImports,
              op,
              op.responses.asSequence()
                .flatMap { res -> res.content?.asSequence()?.map { con -> res to con } ?: emptySequence() }
                .map { (res, pair) -> ContentPair(res, ExtendedMediaType.extend(pair.key, pair.value)) }
                .toList()
            )
          }
        }.forEach {
          (templatingEngine as MustacheEngineAdapter).compiler
            .compile(tplMan.getFullTemplateContents("union/impl.mustache"))
            .execute(it)
            .also { res -> tplMan.writeToFile(
              modelFileFolder() + File.separator + "${StringUtils.capitalize(it.operation.nickname)}UnionResponse.kt",
              res,
            ) }
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
        BreakState.Break    -> return state
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
    datatype = type // known to be deprecated, here for compatibility
  }

  @Suppress("UNCHECKED_CAST")
  private fun MutableMap<String, Map<Any, String>>.getOrComputeDefaultValue(
    name: String,
    disc: CodegenModel
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
