rootProject.name = "openapi-jaxrs-kt"

include(":jaxrs-kt")
project(":jaxrs-kt").apply {
  name = "jaxrs-kt"
  projectDir = file("plugins/kotlin/jaxrs")
}
