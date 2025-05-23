import org.gradle.jvm.tasks.Jar

plugins {
  `maven-publish`
}

group = "org.veupathdb.lib"

publishing {
  repositories {
    maven {
      name = "GitHub"
      url = uri("https://maven.pkg.github.com/VEuPathDB/lib-openapi-generator-plugins")
      credentials {
        username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
        password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
      }
    }

    publications {
      subprojects.forEach { generator ->
        create<MavenPublication>(project.name) {
          from(generator.components["java"])

          artifactId = "oas-${generator.name}"
          version = generator.version as String

          pom {
            name = "oas-${generator.name}"
            url = "https://github.com/VEuPathDB/lib-openapi-generator-plugins"

            licenses { license { name = "Apache-2.0" } }

            developers {
              developer {
                id = "epharper"
                name = "Elizabeth Paige Harper"
                email = "elizabeth.harper@foxcapades.io"
                url = "https://github.com/foxcapades"
              }
            }
          }
        }
      }
    }
  }
}
