package com.hchjeong.springiscool

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@ConfigurationPropertiesScan
@SpringBootApplication
class SpringIsCoolApplication

fun main(args: Array<String>) {
    runApplication<SpringIsCoolApplication>(*args)
}