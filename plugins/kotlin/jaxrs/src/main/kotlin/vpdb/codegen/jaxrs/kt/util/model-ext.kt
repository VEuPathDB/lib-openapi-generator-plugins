package vpdb.codegen.jaxrs.kt.util

import org.openapitools.codegen.CodegenModel
import org.openapitools.codegen.CodegenProperty

fun CodegenModel.streamVars() =
  sequenceOf(
    allVars,
    vars,
    requiredVars,
    optionalVars,
    readWriteVars,
    readOnlyVars,
    nonNullableVars,
    parentRequiredVars,
    parentVars,
  )
    .filterNotNull()
    .flatMap(Iterable<CodegenProperty>::asSequence)
