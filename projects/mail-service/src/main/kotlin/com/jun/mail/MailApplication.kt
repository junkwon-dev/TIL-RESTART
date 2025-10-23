package com.jun.mail

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.resilience.annotation.EnableResilientMethods

@SpringBootApplication
@EnableResilientMethods
class mailApplication

fun main(args: Array<String>) {
	runApplication<mailApplication>(*args)
}
