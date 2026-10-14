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

    should("use russian interface for the chosen russian and english for any other chosen language") {
        LocaleUtils.resolve(user("en", chosen = "ru")) shouldBe ru
        DataLocale.SUPPORTED.filter { it != "ru" }.forEach {
            LocaleUtils.resolve(user("ru", chosen = it)) shouldBe en
        }
    }

    should("ignore a stored language that is not supported") {
        LocaleUtils.resolve(user("ru", chosen = "xx")) shouldBe ru
    }
})
