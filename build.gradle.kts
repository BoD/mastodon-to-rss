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
//        implementation(Ktor.server.defaultHeaders)
                implementation(Ktor.server.statusPages)
                implementation(Ktor.client.core)
                implementation(Ktor.client.curl)
                implementation(Ktor.client.contentNegotiation)
//                implementation("org.redundent:kotlin-xml-builder:_")
                implementation(KotlinX.serialization.json)
                implementation(Ktor.plugins.serialization.kotlinx.json)
//                runtimeOnly("ch.qos.logback:logback-classic:_")
            }
        }
    }
}

// `./gradlew refreshVersions` to update dependencies
