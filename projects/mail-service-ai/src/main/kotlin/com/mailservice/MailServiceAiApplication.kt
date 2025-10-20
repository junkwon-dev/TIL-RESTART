package com.mailservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

/**
 * Main entry point for the Mail Service AI Spring Boot application.
 * Enables asynchronous method execution.
 */
@SpringBootApplication
@EnableAsync
class MailServiceAiApplication

/**
 * Runs the Mail Service AI Spring Boot application.
 *
 * @param args Command line arguments.
 */
fun main(args: Array<String>) {
	runApplication<MailServiceAiApplication>(*args)
}
