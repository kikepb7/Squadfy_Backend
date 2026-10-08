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

    testImplementation(kotlin("test"))
    testImplementation(libs.spring.boot.data.jpa.test)
    testImplementation(testFixtures(projects.common))
}
