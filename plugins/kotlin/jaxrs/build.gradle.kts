import org.jetbrains.kotlin.gradle.dsl.JvmTarget

version = "1.0.0-SNAPSHOT"

plugins {
  kotlin("jvm") version "2.1.21"
  `maven-publish`
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(libs.openapi.codegen)
}

java {
  targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
  compilerOptions {
    jvmTarget = JvmTarget.JVM_11
  }
}

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
      create<MavenPublication>(name) {
        from(components["java"])

        artifactId = "oas-${name}"

        pom {
          name = "oas-${name}"
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
