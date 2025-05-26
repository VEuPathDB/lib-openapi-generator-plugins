package vpdb.codegen.jaxrs.kt.ext

import java.lang.reflect.Field

@Suppress("UNCHECKED_CAST")
internal fun Field.handleFinal(og: Any, new: Any) {
  when {
    MutableCollection::class.java.isAssignableFrom(type) -> {
      val col = get(new) as MutableCollection<Any>
      col.clear()
      col.addAll(get(og) as MutableCollection<Any>)
    }

    MutableMap::class.java.isAssignableFrom(type) -> {
      val map = get(new) as MutableMap<Any, Any>
      map.clear()
      map.putAll(get(og) as Map<out Any, Any>)
    }
  }
}

internal fun Field.handlePlain(og: Any, new: Any) = set(new, get(og))