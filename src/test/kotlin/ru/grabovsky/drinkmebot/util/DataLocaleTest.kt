package ru.grabovsky.drinkmebot.util

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import java.util.Locale

class DataLocaleTest : ShouldSpec({
    should("map english to us") {
        DataLocale.of(Locale.forLanguageTag("en")) shouldBe "us"
        DataLocale.of(Locale.forLanguageTag("en-GB")) shouldBe "us"
    }

    should("map telegram uk (Ukrainian) to ua") {
        DataLocale.of(Locale.forLanguageTag("uk")) shouldBe "ua"
    }

    should("keep other supported languages as is") {
        listOf("ru", "de", "es", "fr", "it").forEach {
            DataLocale.of(Locale.forLanguageTag(it)) shouldBe it
        }
    }

    should("fall back to us for unsupported languages") {
        DataLocale.of(Locale.forLanguageTag("pl")) shouldBe "us"
    }
})
