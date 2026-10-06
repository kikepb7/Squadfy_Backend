plugins {
	id("squadfy.spring-boot-app")
}

description = "Squadfy Backend"

dependencies {
	implementation(projects.user)
	implementation(projects.chat)
	implementation(projects.notification)
	implementation(projects.club)
	implementation(projects.match)
	implementation(projects.common)

	implementation(libs.kotlin.reflect)
	implementation(libs.spring.boot.starter.security)

	implementation(libs.spring.boot.starter.data.jpa)
	implementation(libs.spring.boot.starter.mail)
	implementation(libs.spring.boot.starter.redis)
	implementation(libs.spring.boot.starter.amqp)
    implementation(libs.jackson.datatype)
	runtimeOnly(libs.postgresql)

	implementation(libs.spring.boot.starter.flyway)
	implementation(libs.spring.boot.starter.actuator)
	implementation(libs.springdoc.openapi.webmvc.ui)
	runtimeOnly(libs.flyway.database.postgresql)

	testImplementation(libs.spring.boot.data.jpa.test)
	testImplementation(testFixtures(projects.common))
}