import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.serialization)
}

group = "org.jraf"
version = "1.0.0"

repositories {
  mavenCentral()
}

kotlin {
  jvm {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_25)
    }
  }
  macosArm64 {
    binaries {
      executable {
        entryPoint = "org.jraf.mastodontorss.main"
      }
    }
  }
  linuxX64 {
    binaries {
      executable {
        entryPoint = "org.jraf.mastodontorss.main"
      }
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.ktor.server.core)
        implementation(libs.ktor.server.cio)
        implementation(libs.ktor.server.statusPages)
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.contentNegotiation)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.ktor.serialization.kotlinx.json)
      }
    }
    macosArm64Main {
      dependencies {
        implementation(libs.ktor.client.curl)
      }
    }
    linuxX64Main {
      dependencies {
        implementation(libs.ktor.client.curl)
      }
    }
    jvmMain {
      dependencies {
        implementation(libs.ktor.client.okhttp)
      }
    }

    jvmTest {
      dependencies {
        implementation(libs.kotlin.test.junit)
      }
    }
  }
}

// `./gradlew refreshVersions` to update dependencies
