plugins {
    id("squadfy.spring-boot-service")
    id("org.springframework.boot")
}

// Exposes name, version and build time through /actuator/info.
springBoot {
    buildInfo()
}

// Stable artifact name for the Docker image; the plain (non-executable) jar is not needed.
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("squadfy.jar")
}

tasks.named<Jar>("jar") {
    enabled = false
}
