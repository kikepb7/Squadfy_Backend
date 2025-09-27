plugins {
    id("java-library")
    id("squadfy.kotlin-common")
}

group = "org.example"
version = "unspecified"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
}

dependencies {
    api(libs.kotlin.reflect)
    api(libs.jackson.module.kotlin)

    implementation(libs.spring.boot.starter.amqp)
    implementation(libs.jackson.datatype)

    implementation(libs.spring.boot.starter.security)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}