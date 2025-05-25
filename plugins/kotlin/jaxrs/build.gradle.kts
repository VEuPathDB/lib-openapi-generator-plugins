import gg.jte.ContentType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

version = "1.0.0-SNAPSHOT"

plugins {
  kotlin("jvm") version "2.1.21"
  `maven-publish`
  id("gg.jte.gradle") version "3.2.1"
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(libs.openapi.codegen) {
    exclude(group = "commons-io")
    exclude(group = "commons-cli")
    exclude(group = "com.fasterxml.jackson.datatype")
    exclude(group = "com.github.ben-manes.caffeine")
    exclude(group = "com.github.curious-odd-man")
    exclude(group = "com.github.jknack")
    exclude(group = "com.github.joschi.jackson")
    exclude(group = "com.github.mifmif")
    exclude(group = "com.google.guava")
    exclude(group = "com.samskivert")
    exclude(group = "net.java.dev.jna")
    exclude(group = "org.apache.commons")
    exclude(group = "org.apache.maven.resolver")
    exclude(group = "org.commonmark")
    exclude(group = "org.projectlombok")
    exclude(group = "org.slf4j")
    exclude(group = "org.yaml")
  }
  implementation(libs.jte.core)
  compileOnly(libs.jte.kt)
}

jte {
  binaryStaticContent = true
  contentType = ContentType.Plain
  trimControlStructures = true
  generate()
}

java {
  targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
  compilerOptions {
    jvmTarget = JvmTarget.JVM_17
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
