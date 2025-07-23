package vpdb.codegen.jaxrs.kt.util

fun cleanSuffix(get: () -> String?, set: (String) -> Unit) =
  get()?.substringBefore('1')?.substringBefore("allOf")?.also(set)
