rootProject.name = "openapi-generator-plugins"

include(":jaxrs-kt")
project(":jaxrs-kt").apply {
  name = "jaxrs-kt"
  projectDir = file("plugins/kotlin/jaxrs")
}
