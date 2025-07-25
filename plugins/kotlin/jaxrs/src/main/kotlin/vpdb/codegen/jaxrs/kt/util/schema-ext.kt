package vpdb.codegen.jaxrs.kt.util

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.databind.ObjectMapper
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

fun Schema<*>.writeTo(path: Path) {
  path.writeText(schemaMapper.writeValueAsString(this)
    .replace(refPat) { it.groupValues[1] + ".json" })
}