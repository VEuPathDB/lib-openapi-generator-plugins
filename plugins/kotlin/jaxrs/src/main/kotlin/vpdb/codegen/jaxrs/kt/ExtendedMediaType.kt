package vpdb.codegen.jaxrs.kt

import io.swagger.v3.oas.models.examples.Example
import org.openapitools.codegen.CodegenEncoding
import org.openapitools.codegen.CodegenMediaType
import org.openapitools.codegen.CodegenProperty
import org.openapitools.codegen.SchemaTestCase

@Suppress("MemberVisibilityCanBePrivate")
class ExtendedMediaType: CodegenMediaType {
  val mimeType: String

  private constructor(
    mime: String,
    schema: CodegenProperty,
    encoding: LinkedHashMap<String, CodegenEncoding>?,
    testCases: HashMap<String, SchemaTestCase>?,
  ): super(schema, encoding, testCases) {
    mimeType = mime
  }

  private constructor(
    mime: String,
    schema: CodegenProperty,
    encoding: LinkedHashMap<String, CodegenEncoding>?,
    testCases: HashMap<String, SchemaTestCase>?,
    examples: MutableMap<String, Example>?,
  ): super(schema, encoding, testCases, examples) {
    mimeType = mime
  }

  private constructor(
    mime: String,
    schema: CodegenProperty,
    encoding: LinkedHashMap<String, CodegenEncoding>?,
    testCases: HashMap<String, SchemaTestCase>?,
    example: Any?,
  ): super(schema, encoding, testCases, example) {
    mimeType = mime
  }

  val dataType: String get() = schema.dataType

  fun prefixName() = mimeType.toSafeName(false)

  fun infixName() = mimeType.toSafeName(true)

  companion object {
    private fun String.toSafeName(upperFirst: Boolean): String {
      if (this in mimeTypeNameOverrides) {
        return mimeTypeNameOverrides[this]!!.let {
          if (upperFirst)
            it[0].uppercaseChar() + it.substring(1)
          else
            it
        }
      }

      val sb = StringBuilder(length)

      if (upperFirst)
        sb.append(get(0).uppercaseChar())
      else
        sb.append(get(0))

      var i = 1;
      outer@while (i < length) {
        val c = get(i++)
        if (c.isWord()) {
          sb.append(c)
        } else {
          while (!get(i).isWord()) {
            i++
            if (i == length)
              break@outer
          }

          sb.append(get(i).uppercaseChar())
        }
      }

      return sb.toString()
    }

    fun extend(mime: String, og: CodegenMediaType): ExtendedMediaType {
      val ext = when {
        !(og.examples.isNullOrEmpty()) -> ExtendedMediaType(mime, og.schema, og.encoding, og.testCases, og.examples)
        og.example != null -> ExtendedMediaType(mime, og.schema, og.encoding, og.testCases, og.example)
        else -> ExtendedMediaType(mime, og.schema, og.encoding, og.testCases)
      }
      ext.vendorExtensions = og.vendorExtensions
      return ext
    }
  }
}