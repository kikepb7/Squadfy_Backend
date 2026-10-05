plugins {
    id("squadfy.spring-boot-service")
    id("org.springframework.boot")
}

// Exposes name, version and build time through /actuator/info.
springBoot {
    buildInfo()
}
