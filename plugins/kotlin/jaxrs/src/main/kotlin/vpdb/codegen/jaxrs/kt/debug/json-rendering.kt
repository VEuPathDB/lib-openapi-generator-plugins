package vpdb.codegen.jaxrs.kt.debug

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonFilter
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonIncludeProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.core.util.Separators
import com.fasterxml.jackson.core.util.Separators.Spacing
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider
import io.swagger.v3.core.util.Json
import org.openapitools.codegen.CodegenDiscriminator
import org.openapitools.codegen.CodegenDiscriminator.MappedModel
import org.openapitools.codegen.CodegenModel
import org.openapitools.codegen.CodegenOperation
import org.openapitools.codegen.CodegenParameter
import org.openapitools.codegen.CodegenProperty

fun configureJsonWriter() = Json.mapper()
  .copy()
  .setFilterProvider(SimpleFilterProvider(mapOf(
    "noiseFilter" to SimpleBeanPropertyFilter.SerializeExceptFilter(setOf(
      "_enum",
      "MUSTACHE_PARENT_CONTEXT",
      "apiDocPath",
      "appDescription",
      "appDescriptionWithNewLines",
      "appName",
      "artifactId",
      "artifactVersion",
      "bodyParams",
      "children",
      "constantParams",
      "cookieParams",
      "datatype",
      "dateLibrary",
      "debugModels",
      "debugOperations",
      "defaultValueWithParam",
      "examples",
      "formParams",
      "generateApiDocs",
      "generateApiTests",
      "generateApis",
      "generateModelDocs",
      "generateModelTests",
      "generateModels",
      "generatorClass",
      "gitHost",
      "gitRepoId",
      "gitUserId",
      "groupId",
      "headerParams",
      "implicitHeadersParams",
      "javaxPackage",
      "jsonSchema",
      "lambda",
      "licenseInfo",
      "mandatory",
      "mapping",
      "modelDocPath",
      "modelJson",
      "nonNullableVars",
      "notNullableParams",
      "notes",
      "omitGradleWrapper",
      "optionalParams",
      "optionalVars",
      "packageName",
      "parcelizeModels",
      "parentRequiredVars",
      "parentVars",
      "pathParams",
      "queryParams",
      "readOnlyVars",
      "readWriteVars",
      "releaseNote",
      "requiredAndNotNullableParams",
      "requiredParams",
      "requiredVars",
      "returnProperty",
      "serializableModel",
      "sortModelPropertiesByRequiredFlag",
      "sortParamsByRequiredFlag",
      "unescapedAppDescription",
      "unescapedNotes",
      "useBeanValidation",
      "useJakartaEe",
      "vars",
    )),
  )))
  .addMixIn(Any::class.java, FilterMixin::class.java)
  .addMixIn(CodegenModel::class.java, ModelMixin::class.java)
  .addMixIn(CodegenOperation::class.java, OperationMixin::class.java)
  .addMixIn(CodegenDiscriminator::class.java, DiscriminatorMixin::class.java)
  .apply { setVisibility(serializationConfig.defaultVisibilityChecker
    .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
    .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
    .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)) }
  .writer(DefaultPrettyPrinter()
    .withSeparators(Separators("\n", ':', Spacing.AFTER, ',', Spacing.NONE, "", ',', Spacing.NONE, ""))
    .withObjectIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
    .withArrayIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE))!!

@JsonInclude(value = JsonInclude.Include.NON_DEFAULT)
@JsonFilter("noiseFilter")
abstract class FilterMixin

abstract class OperationMixin: FilterMixin() {
  @field:JsonProperty("parameters")
  val allParams: List<CodegenParameter> = emptyList()
}

abstract class ModelMixin: FilterMixin() {
  @field:JsonProperty("properties")
  val allVars: List<CodegenProperty> = emptyList()
}

abstract class DiscriminatorMixin: FilterMixin() {
  @get:JsonIgnoreProperties("model")
  abstract val mappedModels: Set<MappedModel>
}
