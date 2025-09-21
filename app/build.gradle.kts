plugins {
	id("squadfy.spring-boot-app")
}

group = "com.kikepb"
version = "0.0.1-SNAPSHOT"
description = "Squadfy Backend"

dependencies {
	implementation(projects.user)
	implementation(projects.chat)
	implementation(projects.notification)
	implementation(projects.common)

	implementation(libs.kotlin.reflect)
	implementation(libs.spring.boot.starter.security)

	implementation(libs.spring.boot.starter.data.jpa)
	implementation(libs.spring.boot.starter.redis)
	runtimeOnly(libs.postgresql)
}