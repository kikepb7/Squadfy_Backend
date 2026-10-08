plugins {
    id("java-library")
    id("java-test-fixtures")
    id("squadfy.kotlin-common")
}

dependencies {
    api(libs.kotlin.reflect)
    api(libs.jackson3.module.kotlin)

    implementation(libs.spring.boot.starter.amqp)
    implementation(libs.spring.boot.starter.web)

    implementation(libs.spring.boot.starter.security)

    implementation(libs.jwt.api)
    runtimeOnly(libs.jwt.impl)
    runtimeOnly(libs.jwt.jackson)

    testImplementation(kotlin("test"))

    // Shared Testcontainers configurations for the other modules' tests (src/testFixtures)
    testFixturesApi(libs.spring.boot.test)
    testFixturesApi(libs.spring.boot.testcontainers)
    testFixturesApi(libs.testcontainers.postgresql)
    testFixturesApi(libs.testcontainers.rabbitmq)
}
