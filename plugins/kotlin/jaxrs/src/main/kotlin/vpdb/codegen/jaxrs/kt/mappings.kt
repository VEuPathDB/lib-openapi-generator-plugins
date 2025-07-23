package vpdb.codegen.jaxrs.kt

val dataTypeMapping = mapOf(
  "string" to "String",
  "boolean" to "Boolean",
  "integer" to "Int",
  "float" to "Float",
  "long" to "Long",
  "double" to "Double",
  "ByteArray" to "ByteArray",
  "array" to "List",
  "list" to "List",
  "set" to "Set",
  "map" to "Map",
  "object" to "Any",
  "binary" to "ByteArray",
  "AnyType" to "Any",
  "number" to "java.math.BigDecimal",
  "decimal" to "java.math.BigDecimal",
  "date-time" to "java.time.OffsetDateTime",
  "date" to "java.time.LocalDate",
  "Date" to "java.time.LocalDate",
  "DateTime" to "java.time.OffsetDateTime",
  "UnsignedLong" to "ULong",
  "UnsignedInteger" to "UInt",
)

val formatMapping = mapOf(
  "string" to mapOf(
    "date-time" to "java.time.OffsetDateTime",
    "date" to "java.time.LocalDate",
    "time" to "java.time.OffsetTime",
    "uri" to "java.net.URI",
    "iri" to "java.net.URI",
    "hostname" to "java.net.URL",
    "uuid" to "java.util.UUID",
    "regex" to "Regex",
    "binary" to "ByteArray",
    "file" to "java.io.File",
    "*" to "String",
  ),
  "integer" to mapOf(
    "int32" to "Int",
    "int64" to "Long",
    "uint32" to "UInt",
    "uint64" to "ULong",
    "big" to "java.math.BigInteger",
    "int8" to "Byte",
    "uint8" to "UByte",
    "int16" to "Short",
    "uint16" to "UShort",
    "*" to "Int",
  ),
  "number" to mapOf(
    "float" to "Float",
    "double" to "Double",
    "float32" to "Float",
    "float64" to "Double",
    "big" to "java.math.BigDecimal",
    "decimal" to "java.math.BigDecimal",
    "*" to "Double",
  ),
)

val kotlinBuiltIns = setOf(
  "Byte",
  "Short",
  "Int",
  "Long",

  "Float",
  "Double",

  "Char",
  "Boolean",

  "String",

  "UByte",
  "UShort",
  "UInt",
  "ULong",

  "Array",

  "ByteArray",
  "ShortArray",
  "IntArray",
  "LongArray",

  "BooleanArray",

  "UByteArray",
  "UShortArray",
  "UIntArray",
  "ULongArray",

  "List",
  "MutableList",

  "Map",
  "MutableMap",

  "Set",
  "MutableSet",
)

val implementations = mapOf(
  "array" to "ArrayList",
  "list"  to "ArrayList",
  "map"   to "LinkedHashMap",
)