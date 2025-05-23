package org.openapitools.codegen

import org.openapitools.codegen.languages.KotlinServerCodegen
import org.openapitools.codegen.model.ModelMap
import org.openapitools.codegen.model.ModelsMap
import org.openapitools.codegen.model.OperationsMap
import org.openapitools.codegen.utils.ModelUtils

class VPDBJaxRSKotlinGenerator: KotlinServerCodegen(), CodegenConfig {

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

    addMustacheLambdas().put("indent") { frag, out ->
      frag.execute().takeUnless { it.isNullOrEmpty() }
        ?.also {
          val buffer = out.buffered()
          it.lineSequence().forEach { buffer.append("  ").append(it).appendLine() }
          buffer.flush()
        }
    }
  }

  override fun processOpts() {
    super.processOpts()
    setLibrary(Constants.JAXRS_SPEC)
  }

  override fun postProcessAllModels(models: Map<String, ModelsMap>): Map<String, ModelsMap> {
    return super.postProcessAllModels(models).also { out ->
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
  }

  private fun CodegenModel.processUnsigned() {
    requiredVars.processUnsigned()
    optionalVars.processUnsigned()
    allVars.processUnsigned()
    if (allVars !== vars)
      vars.processUnsigned()
  }

  private fun Iterable<CodegenProperty>.processUnsigned() {
    asSequence()
      .filterNot { it.vendorExtensions.isNullOrEmpty() }
      .filter { it.vendorExtensions["x-unsigned"] == true }
      .forEach {
        when {
          it.isLong    -> it.processUnsigned("ULong")
          it.isInteger -> it.processUnsigned("UInt")
        }
      }
  }

  private fun CodegenProperty.processUnsigned(type: String) {
    dataType = type
    datatypeWithEnum = type
    baseType = type
    datatype = type // known to be deprecated, here for compatibility
  }

  override fun postProcessOperationsWithModels(operations: OperationsMap, allModels: MutableList<ModelMap>): OperationsMap {
    return super.postProcessOperationsWithModels(operations, allModels)
      .apply {
        imports = imports
          .filterNotTo(ArrayList(8)) { it["import"]?.startsWith(modelPackage) == true }
          .also { it.add(mapOf("import" to "$modelPackage.*")) }
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
    vendorExtensions["x-has-data-class-body"] = true
    requiredVars.processProperties(discField, discValue, disc, enumDiscriminators)
    optionalVars.processProperties(discField, discValue, disc, enumDiscriminators)
    vars.processProperties(discField, discValue, disc, enumDiscriminators)
    if (allVars !== vars) {
      allVars.processProperties(discField, discValue, disc, enumDiscriminators)
    }
  }

  private fun List<CodegenProperty>.processProperties(
    baseName: String,
    discValue: String,
    disc: CodegenModel,
    enumDiscriminators: MutableMap<String, Map<Any, String>>
  ) {
    firstOrNull { it.defaultValue == null && it.baseName == baseName }
      ?.also {
        it.defaultValue = enumDiscriminators.getOrComputeDefaultValue(discValue, disc)
        it.isReadOnly = true
      }
  }

  private fun CodegenModel.processRawDiscriminator() {
    if (defaultValue == null)
      defaultValue = vendorExtensions["x-discriminator-value"].toString()
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
}
