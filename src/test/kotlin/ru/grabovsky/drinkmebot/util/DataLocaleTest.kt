package ru.grabovsky.drinkmebot.util

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import java.util.Locale

class DataLocaleTest : ShouldSpec({
    should("map english to us") {
        DataLocale.of(Locale.forLanguageTag("en")) shouldBe "us"
        DataLocale.of(Locale.forLanguageTag("en-GB")) shouldBe "us"
    }

    should("treat telegram uk (Ukrainian) as unsupported while it is disabled") {
        DataLocale.of(Locale.forLanguageTag("uk")) shouldBe "us"
        ("ua" in DataLocale.SUPPORTED) shouldBe false
    }

    should("keep other supported languages as is") {
        listOf("ru", "de", "es", "fr", "it").forEach {
            DataLocale.of(Locale.forLanguageTag(it)) shouldBe it
        }
    }

    should("have a flag for every supported locale") {
        DataLocale.SUPPORTED_ORDERED.forEach { DataLocale.flag(it).isNotEmpty() shouldBe true }
        DataLocale.flag("uk") shouldBe "🇬🇧"
    }

    should("fall back to us for unsupported languages") {
        DataLocale.of(Locale.forLanguageTag("pl")) shouldBe "us"
    }
})
