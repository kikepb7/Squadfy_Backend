import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension

plugins {
    id("squadfy.kotlin-common")
    id("io.spring.dependency-management")
}

// JPA entities must be open so Hibernate can create lazy-loading proxies.
configure<AllOpenExtension> {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

dependencies {
    "implementation"(libraries.findLibrary("kotlin-reflect").get())
    "implementation"(libraries.findLibrary("kotlin-stdlib").get())
    "implementation"(libraries.findLibrary("spring-boot-starter-web").get())
    "implementation"(libraries.findLibrary("swagger-annotations").get())

    "testImplementation"(libraries.findLibrary("spring-boot-starter-test").get())
    "testImplementation"(libraries.findLibrary("kotlin-test-junit5").get())
    "testRuntimeOnly"(libraries.findLibrary("junit-platform-launcher").get())
}
