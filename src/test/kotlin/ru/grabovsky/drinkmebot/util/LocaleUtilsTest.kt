package ru.grabovsky.drinkmebot.util

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import ru.grabovsky.drinkmebot.entity.User
import ru.grabovsky.drinkmebot.entity.UserProfile
import java.util.Locale

class LocaleUtilsTest : ShouldSpec({
    val ru = Locale.forLanguageTag("ru")
    val en = Locale.forLanguageTag("en")

    fun user(telegramLanguage: String?, chosen: String? = null) =
        User(1L, "T", null, "t", telegramLanguage).apply {
            profile = UserProfile(user = this).also { it.settings.cocktailLang = chosen }
        }

    should("use the Telegram language when no bot language is chosen") {
        LocaleUtils.resolve(user("ru")) shouldBe ru
        LocaleUtils.resolve(user("en")) shouldBe en
        LocaleUtils.resolve(user("de")) shouldBe Locale.forLanguageTag("de")
        LocaleUtils.resolve(null) shouldBe ru
    }

    should("use the interface language of the chosen bot language") {
        LocaleUtils.resolve(user("en", chosen = "ru")) shouldBe ru
        listOf("us", "uk").forEach { LocaleUtils.resolve(user("ru", chosen = it)) shouldBe en }
        mapOf("de" to "de", "es" to "es", "fr" to "fr", "it" to "it").forEach { (chosen, interfaceLanguage) ->
            LocaleUtils.resolve(user("ru", chosen = chosen)) shouldBe Locale.forLanguageTag(interfaceLanguage)
        }
    }

    should("show english interface to telegram uk users while Ukrainian is disabled") {
        LocaleUtils.resolve(user("uk")) shouldBe en
        LocaleUtils.resolve(user("ru", chosen = "ua")) shouldBe ru
    }

    should("map every interface language back to the same data language") {
        DataLocale.SUPPORTED_ORDERED.filter { it != "uk" }.forEach { DataLocale.of(LocaleUtils.interfaceLocale(it)) shouldBe it }
    }

    should("ignore a stored language that is not supported") {
        LocaleUtils.resolve(user("ru", chosen = "xx")) shouldBe ru
    }
})
