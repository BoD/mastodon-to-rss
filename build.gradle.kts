import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

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
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_11)
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

  // See https://kotlinlang.org/docs/native-memory-manager.html#memory-consumption
  targets.withType<KotlinNativeTarget> {
    binaries.all {
      freeCompilerArgs = freeCompilerArgs + "-Xallocator=std"
    }
  }
}

// `./gradlew refreshVersions` to update dependencies
