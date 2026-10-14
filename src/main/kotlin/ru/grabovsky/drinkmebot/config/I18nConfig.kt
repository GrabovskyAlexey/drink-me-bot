package ru.grabovsky.drinkmebot.config

import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ReloadableResourceBundleMessageSource

@Configuration
class I18nConfig {

    @Bean
    fun messageSource(): MessageSource {
        val messageSource = ReloadableResourceBundleMessageSource()
        messageSource.setBasename("classpath:messages")
        messageSource.setCacheSeconds(3600)
        messageSource.setDefaultEncoding("UTF-8")
        // Неподдерживаемые языки получают английский (messages.properties), а не язык системы
        messageSource.setFallbackToSystemLocale(false)
        return messageSource
    }
}