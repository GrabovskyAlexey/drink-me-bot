package ru.grabovsky.drinkmebot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class DrinkMeBotApplication

fun main(args: Array<String>) {
    runApplication<DrinkMeBotApplication>(*args)
}
