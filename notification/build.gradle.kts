plugins {
    id("java-library")
    id("squadfy.spring-boot-service")
    kotlin("plugin.jpa")
}

dependencies {
    implementation(projects.common)

    implementation(libs.firebase.admin.sdk)

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.mail)
    implementation(libs.spring.boot.starter.amqp)
    implementation(libs.spring.boot.starter.thymeleaf)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)

    runtimeOnly(libs.postgresql)

    testImplementation(kotlin("test"))
}
