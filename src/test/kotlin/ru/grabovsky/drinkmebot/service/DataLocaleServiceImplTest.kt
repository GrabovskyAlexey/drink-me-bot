package ru.grabovsky.drinkmebot.service

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import ru.grabovsky.drinkmebot.entity.User
import ru.grabovsky.drinkmebot.entity.UserProfile
import ru.grabovsky.drinkmebot.service.interfaces.UserService
import java.util.Locale

class DataLocaleServiceImplTest : ShouldSpec({
    val userService = mockk<UserService>(relaxed = true)
    val service = DataLocaleServiceImpl(userService)
    val ru = Locale.forLanguageTag("ru")
    val en = Locale.forLanguageTag("en")
    lateinit var user: User

    beforeTest {
        clearMocks(userService)
        user = User(7L, "T", null, "t").apply { profile = UserProfile(user = this) }
        every { userService.getUser(7L) } returns user
    }

    should("follow the interface language when nothing is chosen") {
        service.resolve(7L, ru) shouldBe "ru"
        service.resolve(7L, en) shouldBe "us"
        service.chosen(7L).shouldBeNull()
    }

    should("prefer the chosen language over the interface language") {
        user.profile!!.settings.cocktailLang = "uk"

        service.resolve(7L, ru) shouldBe "uk"
        service.chosen(7L) shouldBe "uk"
    }

    should("ignore a stored language that is no longer supported") {
        user.profile!!.settings.cocktailLang = "xx"

        service.resolve(7L, ru) shouldBe "ru"
        service.chosen(7L).shouldBeNull()
    }

    should("save the chosen language into the profile") {
        service.choose(7L, "de")

        user.profile!!.settings.cocktailLang shouldBe "de"
        verify { userService.saveUser(user) }
    }

    should("reset to auto mode") {
        user.profile!!.settings.cocktailLang = "de"

        service.choose(7L, null)

        user.profile!!.settings.cocktailLang.shouldBeNull()
        verify { userService.saveUser(user) }
    }

    should("not save an unknown language or an unknown user") {
        service.choose(7L, "xx")
        every { userService.getUser(8L) } returns null
        service.choose(8L, "ru")

        verify(exactly = 0) { userService.saveUser(any()) }
    }

    should("fall back to the interface language for an unknown user") {
        every { userService.getUser(9L) } returns null

        service.resolve(9L, ru) shouldBe "ru"
    }
})
