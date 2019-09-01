package com.victor.oancea.hermes.starter

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@EnableConfigurationProperties
@SpringBootApplication(scanBasePackages = ["com.victor.oancea"])
class HermesApplication

fun main(args: Array<String>) {
    runApplication<HermesApplication>(*args)
}
