plugins {
  kotlin("multiplatform")
  kotlin("plugin.serialization")
}

group = "org.jraf"
version = "1.0.0"

repositories {
  mavenCentral()
}

kotlin {
  jvm {
    compilations.all {
      kotlinOptions {
        jvmTarget = "1.8"
      }
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
    val commonMain by getting {
      dependencies {
        implementation(Ktor.server.core)
        implementation(Ktor.server.cio)
        implementation(Ktor.server.statusPages)
        implementation(Ktor.client.core)
        implementation(Ktor.client.contentNegotiation)
        implementation(KotlinX.serialization.json)
        implementation(Ktor.plugins.serialization.kotlinx.json)
      }
    }
    val macosArm64Main by getting {
      dependencies {
        implementation(Ktor.client.curl)
      }
    }
    val linuxX64Main by getting {
      dependencies {
        implementation(Ktor.client.curl)
      }
    }
    val jvmMain by getting {
      dependencies {
        implementation(Ktor.client.okHttp)
      }
    }

    val jvmTest by getting {
      dependencies {
        implementation(Kotlin.test.junit)
      }
    }
  }
}

// `./gradlew refreshVersions` to update dependencies
