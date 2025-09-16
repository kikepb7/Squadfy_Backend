package com.kikepb.squadfy

import chat.Test
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SquadfyApplication

fun main(args: Array<String>) {
	Test()
	runApplication<SquadfyApplication>(*args)
}
