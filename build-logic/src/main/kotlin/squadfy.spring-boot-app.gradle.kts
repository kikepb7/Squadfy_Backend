import gradle.kotlin.dsl.accessors._3ef9c89b436b2435895044c4cd9d19d0.allOpen
import gradle.kotlin.dsl.accessors._3ef9c89b436b2435895044c4cd9d19d0.java

plugins {
    id("squadfy.spring-boot-service")
    id("org.springframework.boot")
    kotlin("plugin.spring")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}