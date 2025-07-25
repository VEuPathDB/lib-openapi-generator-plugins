package vpdb.codegen.jaxrs.kt

import com.fasterxml.jackson.databind.ObjectWriter
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import org.openapitools.codegen.*
import org.openapitools.codegen.languages.KotlinServerCodegen
import org.openapitools.codegen.model.ModelMap
import org.openapitools.codegen.model.ModelsMap
import org.openapitools.codegen.model.OperationMap
import org.openapitools.codegen.model.OperationsMap
import org.openapitools.codegen.utils.ModelUtils
import vpdb.codegen.jaxrs.kt.debug.configureJsonWriter
import vpdb.codegen.jaxrs.kt.ext.ExtendedMediaType
import vpdb.codegen.jaxrs.kt.tpl.needsWrapperResponse
import vpdb.codegen.jaxrs.kt.tpl.usesJaxRSResponse
import vpdb.codegen.jaxrs.kt.util.*
import java.io.File
import java.nio.file.Path
import java.util.IdentityHashMap
import kotlin.io.path.Path
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createDirectories

class VPDBJaxRSKotlinGenerator: KotlinServerCodegen(), CodegenConfig {
  /**
   * Packages that the underlying implementation will not resolve to imports
   * (they will use inline package paths), that we will translate and create
   * imports for.
   */
  private val importCorrectionPackages = arrayOf("java.io.", "java.net.", "java.time.")

  private lateinit var jsonWriter: ObjectWriter

  private var generateSupportTypes = false

  private var debugModels: Path? = null
  private var debugOperations: Path? = null

  private val operations = mutableSetOf<OperationMap>()

  private val annotationsPackage
    by lazy { modelPackage.substringBeforeLast('.') + ".annotations" }

  private lateinit var resourceDir: Path

  init {
    library = Constants.JAXRS_SPEC

    languageSpecificPrimitives = kotlinBuiltIns
    defaultIncludes = kotlinBuiltIns
    typeMapping = dataTypeMapping
    instantiationTypes = implementations

    templateDir = null
    embeddedTemplateDir = null

    modelTemplateFiles.clear()
    modelTemplateFiles["model/model.kte"] = ".kt"

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

    jsonWriter = configureJsonWriter()

    additionalProperties[Constants.INTERFACE_ONLY] = true
    additionalProperties[Constants.OMIT_GRADLE_WRAPPER] = true

    resourceDir = if ("resourceFolder" in additionalProperties) {
      Path(outputDir).resolve(additionalProperties["resourceFolder"] as String)
    } else {
      Path(outputDir).resolve(sourceFolder).parent.resolve("resources")
    }

    enablePostProcessFile = true
  }

  override fun toDefaultValue(cp: CodegenProperty, schema: Schema<*>): String? {
    return if (ModelUtils.isURISchema(schema)) {
      if (schema.default != null) {
        "URI.create(\"${schema.default}\")"
      } else {
        null
      }
    } else {
      super.toDefaultValue(cp, schema)
    }
  }

  override fun postProcessModels(objs: ModelsMap): ModelsMap {
    super.postProcessModels(objs)

    val model = (objs.models.firstOrNull { it != null } ?: return objs).model

    model.fixTypes()

    // Bug in the generator, this return value isn't used.
    return objs
  }

  override fun fromProperty(
    name: String,
    p: Schema<*>,
    required: Boolean,
    schemaIsFromAdditionalProperties: Boolean,
  ): CodegenProperty =
    PropValue(super.fromProperty(name, p, required, schemaIsFromAdditionalProperties))
      .apply { processProperty() }
      .value

  override fun preprocessOpenAPI(openAPI: OpenAPI) {
    super.preprocessOpenAPI(openAPI)

    openAPI.paths.forEach { (_, body) ->
      body.readOperations()?.forEach {
        it.tags?.let { tags ->
          if (tags.size > 1) {
            val last = tags.last()
            tags.clear()
            tags.add(last)
          }
        }
      }
    }
  }

  override fun postProcessModelProperty(model: CodegenModel, property: CodegenProperty) {
    super.postProcessModelProperty(model, property)

    property.cleanTypeNames()

    if (property.baseType.contains('.')) {
      model.imports.add(property.baseType)
      if (property.baseType.startsWith("java.time."))
        model.imports.add("com.fasterxml.jackson.annotation.JsonFormat")
    }
  }

  override fun postProcessParameter(parameter: CodegenParameter) {
    super.postProcessParameter(parameter)

    parameter::dataType.cleanSuffix()
    parameter::baseType.cleanSuffix()
    parameter::defaultValue.cleanSuffix()

    if (parameter.isArray) {
      if (importCorrectionPackages.any { parameter.items!!.baseType.startsWith(it) })
        ParamValue(parameter).overrideArrayType(parameter.items!!.baseType.substringAfterLast('.'))
    } else if (parameter.isContainer) {
      if (parameter.isMap)
        parameter.fixMapKey()
    } else {
      if (importCorrectionPackages.any { parameter.dataType.startsWith(it) })
        ParamValue(parameter).overrideDataType(parameter.dataType.substringAfterLast('.'))
    }
  }

  override fun postProcessAllModels(models: ModelIndex): ModelIndex {
    val it = models.iterator()

    while (it.hasNext()) {
      it.next().also { (key, _) ->
        if (key.hasTypeSuffix())
          it.remove()
      }
    }

    val schemaAnnotationImport = "$annotationsPackage.JsonSchema"

    return (super.postProcessAllModels(models) as ModelIndex)
      .also { out ->
        val enumDiscriminators = HashMap<String, Map<Any, String>>()

        val schemaDir = resourceDir.resolve("schema/rest-api")
        schemaDir.createDirectories()

        out.asSequence()
          .onEach { (_, map) -> map.imports = emptyList() }
          .map { it.key }
          .mapNotNull { ModelUtils.getModelByName(it, models) }
          .onEach { it.imports = it.imports.filterTo(HashSet<String>(3)) { i -> i.contains('.') } }
          .onEach { it.imports.add(schemaAnnotationImport) }
          .onEach {
            it.streamVars()
              .onEach { it::defaultValue.cleanSuffix() }
              .onEach { it::defaultValueWithParam.cleanSuffix() }
              .forEach(CodegenProperty::cleanTypeNames)
          }
          .onEach {
            if (it.discriminator != null) {
              it.imports.add("com.fasterxml.jackson.annotation.JsonIgnoreProperties")
              it.imports.add("com.fasterxml.jackson.annotation.JsonSubTypes")
              it.imports.add("com.fasterxml.jackson.annotation.JsonTypeInfo")
            }
          }
          .onEach {
            openAPI.components.schemas[it.schemaName]!!
              .writeTo(schemaDir.resolve(it.schemaName + ".json"))
          }
          .filterNot { it.parentModel?.discriminator == null }
          .filterNot { it.parentModel.discriminator.propertyType in languageSpecificPrimitives }
          .forEach { it.processDiscriminator(models, enumDiscriminators) }
      }
      .also { out -> debugModels?.bufferedWriter()?.use { jsonWriter.writeValue(it, out) } }
  }

  override fun postProcessResponseWithProperty(response: CodegenResponse, property: CodegenProperty) {
    super.postProcessResponseWithProperty(response, property)
    response.content?.replaceAll(ExtendedMediaType.Companion::extend)
  }

  override fun postProcessOperationsWithModels(allOps: OperationsMap, allModels: MutableList<ModelMap>) =
    allOps
      .apply { put(ModelPackageKey, modelPackage) }
      .also { ops -> ops.operations.operation.forEach { if (it.needsWrapperResponse) generateSupportTypes = true } }
      .also { if (generateSupportTypes) it[SupportPackageKey] = "${modelPackage}.support.*" }
      .apply {
        operations.operation.forEach { op ->
          op.responses?.forEach { res ->
            res::dataType.cleanSuffix()
            res::baseType.cleanSuffix()
          }

          if (op.needsWrapperResponse) {
            op.responseHeaders.asSequence()
              .filter { header -> importCorrectionPackages.any { header.baseType.startsWith(it) } }
              .forEach { op.imports.add(it.baseType) }
          }
        }
      }
      .apply { patchImports() }
      .also { it.operations?.also(this.operations::add) }
      .also { _ -> debugOperations?.bufferedWriter()?.use { jsonWriter.writeValue(it, this.operations) } }

  override fun postProcess() {
    JteAdaptor.compileTemplate(
      annotationsPackage,
      "extras/json-schema-annotation.kte",
      Path(modelFileFolder())
        .resolveSibling("annotations")
        .apply { createDirectories() }
        .resolve("JsonSchema.kt")
    )

    if (generateSupportTypes) {
      val supportPackage = "$modelPackage.support"
      val supportFolder = Path(modelFileFolder(), "support")

      supportFolder.createDirectories()

      JteAdaptor.compileTemplate(supportPackage, "model/union/def.kte", supportFolder.resolve("AbstractUnionResponse.kt"))

      operations.forEach { operation ->
        operation.operation.asSequence()
          .map {
            it.let { op ->
              UnionImpl(
                supportPackage,
                modelPackage,
                op,
                op.responses.asSequence()
                  .flatMap { res -> res.content?.asSequence()
                    ?.onEach { con -> con.value.schema.also { schema ->
                      schema::dataType.cleanSuffix()
                      schema::baseType.cleanSuffix()
                    } }
                    ?.map { con -> res to con } ?: emptySequence() }
                  .map { (res, pair) -> ContentPair(res, ExtendedMediaType.extend(pair.key, pair.value)) }
                  .toList()
              )
            }
          }.forEach {
            JteAdaptor.compileTemplate(it, "model/union/impl.kte", supportFolder.resolve("Union${it.operation.operationIdCamelCase}Response.kt"))
          }
      }
    }
  }

  override fun postProcessFile(file: File, fileType: String) {
    if (fileType == "model" || fileType == "api") {
      file.writeText(
        file.readText()
          .replace("\n\n\n", "\n\n")
          .replace("\n\n)", "\n)"))
    }
  }

  private fun OperationsMap.patchImports() {
    // Break down the list of maps into a set of import strings to avoid adding
    // duplicate imports.
    val tempImports = imports.asSequence()
      .map { it["import"]!! }
      .toMutableSet()

    operations.operation.forEach {
      if (it.needsWrapperResponse)
        tempImports.add("$modelPackage.support.*")
      if (it.usesJaxRSResponse)
        tempImports.add("jakarta.ws.core.Response")
      if (it.formParams.any { p -> p.baseType == "InputStream" || p.isFile })
        tempImports.add("java.io.InputStream")

      it.allParams.forEach { param ->
        if (importCorrectionPackages.any { param.baseType?.startsWith(it) == true }) {
          tempImports.add(param.baseType)
        }
      }
    }

    imports = tempImports.map { mapOf("import" to it) }
  }

  private fun PropValue.processProperty() {
    when {
      value.isArray -> fixArrayType()
      importCorrectionPackages.any { baseType.startsWith(it) } -> packagePathToImport()
      openApiType in formatMapping -> adjustForFormat()
    }
  }

  private fun PropValue.packagePathToImport() {
    overrideDataType(baseType.substringAfterLast('.'))
  }

  private fun PropValue.adjustForFormat() {
    val newType = formatMapping[openApiType]!![format ?: "*"]
      ?: return

    if (newType.contains('.')) {
      overrideDataType(newType.substringAfterLast('.'))
    } else {
      overrideDataType(newType)
    }

    baseType = newType
  }

  private fun TypedValue.fixArrayType() {
    if (format == "array") {
      var optIn = false

      if (items!!.isNumeric) {
        val primArray = when (dataType) {
          "Integer" -> "IntArray"
          "Long"    -> "LongArray"
          "Double"  -> "DoubleArray"
          "Float"   -> "FloatArray"
          "UInt"    -> "UIntArray"
          "ULong"   -> "ULongArray"
          "Byte"    -> "ByteArray"
          "UByte"   -> "UByteArray"
          "Short"   -> "ShortArray"
          "UShort"  -> "UShortArray"
          else      -> null
        }

        if (primArray != null) {
          overrideDataType(primArray)
          containerTypeMapped = primArray

          if (primArray.startsWith('U')) {
            optIn = true
          }
        } else {
          overrideDataType("Array<${dataType}>")
          containerTypeMapped = "Array"
        }
      } else if (items!!.isBoolean) {
        overrideDataType("BooleanArray")
        containerTypeMapped = "BooleanArray"
      }

      if (optIn) {
        vendorExtensions[KotlinAnnotations] = listOf("@OptIn(ExperimentalUnsignedTypes::class)")
      }
    } else {
      if (importCorrectionPackages.any { baseType.startsWith(it) })
        overrideArrayType(baseType.substringAfterLast('.'))
    }
  }

  private fun CodegenModel.fixTypes() {
    if (isEnum) {
      imports.add("com.fasterxml.jackson.annotation.JsonCreator")
      imports.add("com.fasterxml.jackson.annotation.JsonValue")
      return
    }

    imports.add("com.fasterxml.jackson.annotation.JsonProperty")

    arrayOf(vars, allVars, requiredVars, optionalVars, readOnlyVars, readWriteVars)
      .processProperties {
        @Suppress("UNCHECKED_CAST")
        (it.vendorExtensions[KotlinAnnotations] as List<String>?)?.also(imports::addAll)
        BreakState.Continue
      }
  }

  private fun CodegenModel.processDiscriminator(models: ModelIndex, enumDiscriminators: EnumDiscriminators) {
    val disc = ModelUtils.getModelByName(parentModel.discriminator.propertyType, models)
      ?: return

    if (disc.isEnum)
      processEnumDiscriminator(disc, enumDiscriminators)
    else
      processRawDiscriminator()
  }

  private fun CodegenModel.processEnumDiscriminator(disc: CodegenModel, enumDiscriminators: EnumDiscriminators) {
    val discField = parentModel.discriminator.propertyBaseName
    val discValue = vendorExtensions[VendorExtension.X_DISCRIMINATOR_VALUE.getName()] as String

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

    vendorExtensions[DataClassBodyFlag] = true
  }

  private fun CodegenModel.processRawDiscriminator() {
    if (defaultValue == null)
      defaultValue = vendorExtensions[VendorExtension.X_DISCRIMINATOR_VALUE.name].toString()
  }

  private fun TypedValue.overrideArrayType(newItemType: String) {
    val newArrayType = "$containerTypeMapped<$newItemType>"

    dataType = newArrayType
    dataTypeWithEnum = newArrayType

    items!!.overrideDataType(newItemType)

    baseType = items!!.baseType
  }

  private fun TypedValue.overrideDataType(type: String) {
    dataType = type
    dataTypeWithEnum = type
  }

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

  @Suppress("UNCHECKED_CAST")
  private fun EnumDiscriminators.getOrComputeDefaultValue(name: String, disc: CodegenModel): String {
    get(disc.classname)?.also { return it[name]!! }

    val enumVals = disc.allowableValues["enumVars"] as List<Map<String, Any>>
    val rawVals = disc.allowableValues["values"] as List<Any>

    return enumVals.asSequence()
      .mapIndexed { i, it -> rawVals[i] to "${disc.classname}.${it["name"]}" }
      .toMap()
      .also { put(disc.classname, it) }[name]!!
  }
}
