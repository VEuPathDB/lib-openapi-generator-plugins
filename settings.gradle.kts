rootProject.name = "openapi-jaxrs-kt"


include(":plugins:kotlin:jaxrs")
project(":plugins:kotlin:jaxrs").apply {
  name = "jaxrs-kt"
  projectDir = file("plugins/kotlin/jaxrs")
}
