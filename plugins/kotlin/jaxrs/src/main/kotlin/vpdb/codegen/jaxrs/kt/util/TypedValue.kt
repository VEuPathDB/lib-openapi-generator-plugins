package vpdb.codegen.jaxrs.kt.util

import org.openapitools.codegen.CodegenParameter
import org.openapitools.codegen.CodegenProperty

sealed interface TypedValue {
  val openApiType: String
  val format: String?
  var containerTypeMapped: String?
  var dataType: String
  var baseType: String
  var dataTypeWithEnum: String
  val items: PropValue?

  val isNumeric: Boolean
  val isBoolean: Boolean
}

@JvmInline
value class ParamValue(val value: CodegenParameter): TypedValue {
  override val openApiType: String
    get() = value.schema.openApiType
  override val format: String?
    get() = value.format
  override val items: PropValue?
    get() = value.items?.let(::PropValue)

  override val isNumeric: Boolean
    get() = value.isNumeric
  override val isBoolean: Boolean
    get() = value.isBoolean

  override var containerTypeMapped: String?
    get() = value.containerTypeMapped
    set(v) { value.containerTypeMapped = v }
  override var dataType: String
    get() = value.dataType
    set(v) { value.dataType = v }
  override var baseType: String
    get() = value.baseType
    set(v) { value.baseType = v }
  override var dataTypeWithEnum: String
    get() = value.datatypeWithEnum
    set(v) { value.datatypeWithEnum = v }
}

@JvmInline
value class PropValue(val value: CodegenProperty): TypedValue {
  override val openApiType: String
    get() = value.openApiType
  override val format: String?
    get() = value.format
  override val items: PropValue?
    get() = value.items?.let(::PropValue)

  override val isNumeric: Boolean
    get() = value.isNumeric
  override val isBoolean: Boolean
    get() = value.isBoolean

  override var containerTypeMapped: String?
    get() = value.containerTypeMapped
    set(v) { value.containerTypeMapped = v }
  override var dataType: String
    get() = value.dataType
    set(v) { value.dataType = v }
  override var baseType: String
    get() = value.baseType
    set(v) { value.baseType = v }
  override var dataTypeWithEnum: String
    get() = value.datatypeWithEnum
    set(v) { value.datatypeWithEnum = v }
}
