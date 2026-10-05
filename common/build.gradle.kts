plugins {
    id("java-library")
    id("squadfy.kotlin-common")
}

dependencies {
    api(libs.kotlin.reflect)
    api(libs.jackson.module.kotlin)

    implementation(libs.spring.boot.starter.amqp)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.jackson.datatype)

    implementation(libs.spring.boot.starter.security)

    implementation(libs.jwt.api)
    runtimeOnly(libs.jwt.impl)
    runtimeOnly(libs.jwt.jackson)

    testImplementation(kotlin("test"))
}
