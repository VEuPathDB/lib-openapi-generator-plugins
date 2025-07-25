package vpdb.codegen.jaxrs.kt.util

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider
import io.swagger.v3.oas.models.media.Schema
import vpdb.codegen.jaxrs.kt.debug.FilterMixin
import java.nio.file.Path
import kotlin.io.path.writeText


private val schemaMapper = ObjectMapper()
  .setFilterProvider(SimpleFilterProvider(mapOf(
    "noiseFilter" to SimpleBeanPropertyFilter.SerializeExceptFilter(setOf(
      "title",
      "description",
      "\$comment",
      "contentEncoding",
      "contentMediaType",
      "contentSchema",
      "examples",
      "readOnly",
      "writeOnly",
      "_enum",
      "_default",
      "extensions",
    )),
  )))
  .addMixIn(Any::class.java, FilterMixin::class.java)
  .apply { setVisibility(serializationConfig.defaultVisibilityChecker
    .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
    .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
    .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)) }

private val refPat = Regex("#/components/schemas/(\\w+)")

fun ObjectNode.writeTo(path: Path) {
  path.writeText(schemaMapper.writeValueAsString(this)
    .replace(refPat) { it.groupValues[1] + ".json" })
}

fun ObjectNode.correctSchema() {
  fixTypes()
  fixProperties()
  fixPoly()
  fixDefs()
  fixItems()
  fixUnevaluated()

  remove("exampleSetFlag")
}

private fun ObjectNode.fixTypes() {
  (get("types") as? ArrayNode)
    ?.takeIf { it.size() == 1 }
    ?.also {
      put("type", it[0].textValue())
      remove("types")
    }
}

private fun ObjectNode.fixProperties() {
  (get("properties") as? ObjectNode)?.forEach {
    (it as? ObjectNode)?.correctSchema()
  }
}

private val polyFields = arrayOf("allOf", "anyOf", "oneOf")

private fun ObjectNode.fixPoly() {
  for (field in polyFields) {
    (get(field) as? ArrayNode)?.forEach {
      (it as? ObjectNode)?.correctSchema()
    }
  }
}

private val defFields = arrayOf("\$defs", "definitions")

private fun ObjectNode.fixDefs() {
  for (field in defFields) {
    (get(field) as? ObjectNode)?.forEach { (it as? ObjectNode)?.fixTypes() }
  }
}

private fun ObjectNode.fixItems() {
  (get("items") as? ObjectNode)?.fixTypes()
}

private fun ObjectNode.fixUnevaluated() {
  (get("unevaluatedProperties") as? ObjectNode)
    ?.also { put("unevaluatedProperties", it.get("booleanSchemaValue").booleanValue()) }

  (get("unevaluatedItems") as? ObjectNode)
    ?.also { put("unevaluatedItems", it.get("booleanSchemaValue").booleanValue()) }
}
