plugins {
    id("java-library")
    id("squadfy.spring-boot-service")
    kotlin("plugin.jpa")
}

group = "org.example"
version = "unspecified"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
}

dependencies {
    implementation(projects.common)

    implementation(libs.spring.boot.starter.amqp)
    implementation(libs.spring.boot.starter.thymeleaf)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}