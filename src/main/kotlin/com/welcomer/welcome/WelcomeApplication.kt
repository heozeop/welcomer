package com.welcomer.welcome

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WelcomeApplication

fun main(args: Array<String>) {
    runApplication<WelcomeApplication>(*args)
}
