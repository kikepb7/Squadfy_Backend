package com.kikepb.squadfy

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class SquadfyApplication

fun main(args: Array<String>) {
	runApplication<SquadfyApplication>(*args)
}
