plugins {
    id("java-library")
    id("squadfy.spring-boot-service")
    kotlin("plugin.jpa")
}

dependencies {
    implementation(projects.common)

    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.postgresql)

    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.amqp)
    implementation(libs.jackson.datatype)

    testImplementation(kotlin("test"))
    testImplementation(libs.spring.boot.data.jpa.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
}
