import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile

plugins {
  kotlin("jvm")
  application
  id("com.bmuschko.docker-java-application")
  kotlin("plugin.serialization")
}

group = "org.jraf"
version = "1.0.0"

repositories {
  mavenCentral()
}

dependencies {
  implementation(Ktor.server.core)
  implementation(Ktor.server.netty)
  implementation(Ktor.server.defaultHeaders)
  implementation(Ktor.server.statusPages)
  implementation(Ktor.client.core)
  implementation(Ktor.client.java)
  implementation(Ktor.client.contentNegotiation)
  implementation(Ktor.client.java)
  implementation("org.redundent:kotlin-xml-builder:_")
  implementation(KotlinX.serialization.json)
  implementation(Ktor.plugins.serialization.kotlinx.json)
  runtimeOnly("ch.qos.logback:logback-classic:_")
}

application {
  mainClass.set("org.jraf.mastodontorss.MainKt")
}

docker {
  javaApplication {
    maintainer.set("BoD <BoD@JRAF.org>")
    ports.set(listOf(8080))
    images.add("bodlulu/${rootProject.name}:latest")
    jvmArgs.set(listOf("-Xms16m", "-Xmx128m"))
  }
  registryCredentials {
    username.set(System.getenv("DOCKER_USERNAME"))
    password.set(System.getenv("DOCKER_PASSWORD"))
  }
}

tasks.withType<DockerBuildImage> {
  platform.set("linux/amd64")
}

tasks.withType<Dockerfile> {
  environmentVariable("MALLOC_ARENA_MAX", "4")
}


// `./gradlew refreshVersions` to update dependencies
// `./gradlew distZip` to create a zip distribution
// `DOCKER_USERNAME=<your docker hub login> DOCKER_PASSWORD=<your docker hub password> ./gradlew dockerPushImage` to build and push the image
