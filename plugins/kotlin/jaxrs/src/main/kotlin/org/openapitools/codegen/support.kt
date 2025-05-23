package org.openapitools.codegen

internal val statusCodeMethodNames = mapOf(
  100 to "continue",
  101 to "switchingProtocols",
  102 to "processing",
  103 to "earlyHints",
  200 to "ok",
  201 to "created",
  202 to "accepted",
  203 to "nonAuthoritativeInformation",
  204 to "noContent",
  205 to "resetContent",
  206 to "partialContent",
  207 to "multiStatus",
  208 to "alreadyReported",
  226 to "imUsed",
  300 to "multipleChoices",
  301 to "movedPermanently",
  302 to "found",
  303 to "seeOther",
  304 to "notModified",
  305 to "useProxy",
  307 to "temporaryRedirect",
  308 to "permanentRedirect",
  400 to "badRequest",
  401 to "unauthorized",
  402 to "paymentRequired",
  403 to "forbidden",
  404 to "notFound",
  405 to "methodNotAllowed",
  406 to "notAcceptable",
  407 to "proxyAuthenticationRequired",
  408 to "requestTimeout",
  409 to "conflict",
  410 to "gone",
  411 to "lengthRequired",
  412 to "preconditionFailed",
  413 to "payloadTooLarge",
  414 to "requestUriTooLong",
  415 to "unsupportedMediaType",
  416 to "requestedRangeNotSatisfiable",
  417 to "expectationFailed",
  418 to "imATeapot",
  421 to "misdirectedRequest",
  422 to "unprocessableEntity",
  423 to "locked",
  424 to "failedDependency",
  425 to "tooEarly",
  426 to "upgradeRequired",
  428 to "preconditionRequired",
  429 to "tooManyRequests",
  431 to "requestHeaderFieldsTooLarge",
  444 to "connectionClosedWithoutResponse",
  451 to "unavailableForLegalReasons",
  499 to "clientClosedRequest",
  500 to "internalServerError",
  501 to "notImplemented",
  502 to "badGateway",
  503 to "serviceUnavailable",
  504 to "gatewayTimeout",
  505 to "httpVersionNotSupported",
  506 to "variantAlsoNegotiates",
  507 to "insufficientStorage",
  508 to "loopDetected",
  510 to "notExtended",
  511 to "networkAuthenticationRequired",
  599 to "networkConnectTimeoutError",
)

internal val mimeTypeNameOverrides = mapOf(
  "application/json"          to "json",
  "application/octet-stream"  to "binary",
  "application/pdf"           to "pdf",
  "application/xml"           to "xml",
  "application/zip"           to "zip",
  "text/plain"                to "text",
  "text/csv"                  to "csv",
  "text/html"                 to "html",
  "text/tab-separated-values" to "tsv",
  "text/tsv"                  to "tsv",
  "text/xml"                  to "xml",
)

internal fun Char.isWord() = when (code) { in 97..122, in 48..57, in 65..90, 94 -> true; else -> false }