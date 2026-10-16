package ru.grabovsky.drinkmebot.strategy.flow.admin

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.grabovsky.drinkmebot.config.FreemarkerConfig
import ru.grabovsky.drinkmebot.entity.User
import ru.grabovsky.drinkmebot.entity.UserProfile
import ru.grabovsky.drinkmebot.model.AudienceStats
import ru.grabovsky.drinkmebot.model.CocktailSearchItem
import ru.grabovsky.drinkmebot.model.IngredientStats
import ru.grabovsky.drinkmebot.model.PopularItem
import ru.grabovsky.drinkmebot.model.StatsSnapshot
import ru.grabovsky.drinkmebot.model.WeekStats
import ru.grabovsky.drinkmebot.service.interfaces.CocktailPhotoService
import ru.grabovsky.drinkmebot.service.interfaces.I18nService
import ru.grabovsky.drinkmebot.service.interfaces.StatsService
import ru.grabovsky.drinkmebot.service.interfaces.UserService
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.DeleteMessageIdAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.EditMessageAction
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowContext
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowKeys
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowStateHolder
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowStartContext
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.SendMessageAction
import ru.grabovsky.drinkmebot.strategy.flow.core.templating.FlowTemplateRenderer
import java.time.LocalDate
import java.util.Locale
import org.telegram.telegrambots.meta.api.objects.User as TgUser

class AdminFlowTest : ShouldSpec({
    val snapshot = StatsSnapshot(
        audience = AudienceStats(10, 1, 3, 8, 2, 5, 9),
        ingredients = IngredientStats(usersWithIngredients = 4, totalMarks = 10, usersWithoutReady = 1),
        cocktailViews = 7,
        popularIngredients = listOf(PopularItem("Водка <b>", 4)),
        popularCocktails = listOf(PopularItem("Негрони", 3)),
        weeks = listOf(WeekStats(LocalDate.parse("2026-06-15"), 3, 5)),
    )
    val stats = mockk<StatsService> { every { snapshot(any()) } returns snapshot }
    val i18n = object : I18nService {
        override fun i18n(code: String, locale: Locale, default: String?, vararg args: Any?) = code
    }
    val ru = Locale.forLanguageTag("ru")
    val photos = mockk<CocktailPhotoService>(relaxed = true)
    val tgUser = mockk<TgUser> { every { id } returns 7L }
    val callback = mockk<CallbackQuery> {
        every { id } returns "cb1"
        every { from } returns tgUser
    }

    fun users(isAdmin: Boolean) = mockk<UserService> {
        every { getUser(any()) } returns User(7L, "T", null, "t").apply {
            profile = UserProfile(user = this, isAdmin = isAdmin)
        }
    }

    fun context() = FlowStartContext(mockk<TgUser> { every { id } returns 7L }, ru, null)

    should("send statistics to an admin") {
        val result = AdminFlow(stats, photos, users(isAdmin = true), i18n).start(context())
        result.completed shouldBe false
        result.actions shouldHaveSize 1
        val message = (result.actions.single() as SendMessageAction).message
        message.stepKey shouldBe "main"
        (message.model as AdminView).stats shouldBe snapshot
    }

    should("do nothing for a non-admin") {
        val result = AdminFlow(stats, photos, users(isAdmin = false), i18n).start(context())
        result.completed shouldBe true
        result.actions.shouldBeEmpty()
    }

    fun admin(isAdmin: Boolean = true) = AdminFlow(stats, photos, users(isAdmin), i18n)

    fun context(step: AdminStep, state: AdminState = AdminState()) =
        FlowContext(tgUser, ru, FlowStateHolder(step.key, state, mapOf("main" to 1)))

    fun textMessage(text: String) = mockk<Message> {
        every { this@mockk.text } returns text
        every { photo } returns null
        every { messageId } returns 55
    }

    fun photoMessage(vararg sizes: PhotoSize) = mockk<Message> {
        every { text } returns null
        every { photo } returns sizes.toList()
        every { messageId } returns 56
    }

    fun size(fileId: String, bytes: Int) = mockk<PhotoSize> {
        every { this@mockk.fileId } returns fileId
        every { fileSize } returns bytes
        every { width } returns 10
        every { height } returns 10
    }

    should("open the photo search from the main screen") {
        val result = admin().onCallback(context(AdminStep.MAIN), callback, "PH")!!

        result.stepKey shouldBe "photo_search"
    }

    should("search cocktails by the text sent by the admin and remove that message") {
        every { photos.search("негр") } returns listOf(CocktailSearchItem(5, "Негрони", hasPhoto = true))

        val result = admin().onMessage(context(AdminStep.PHOTO_SEARCH), textMessage("негр"))!!

        result.stepKey shouldBe "photo_pick"
        val message = (result.actions.first() as EditMessageAction).message
        message.inlineButtons.map { it.text to it.payload.data } shouldContainExactly
            listOf("🖼 Негрони" to "P:5", "buttons.common.back" to "M")
        result.actions.filterIsInstance<DeleteMessageIdAction>().single().messageId shouldBe 55
    }

    should("choose a cocktail and remember it for the upload") {
        every { photos.name(5) } returns "Негрони"
        every { photos.fileId(5) } returns null

        val result = admin().onCallback(context(AdminStep.PHOTO_PICK), callback, "P:5")!!

        result.stepKey shouldBe "photo_upload"
        result.payload.cocktailId shouldBe 5
    }

    should("ignore a chosen cocktail that does not exist") {
        every { photos.name(404) } returns null

        admin().onCallback(context(AdminStep.PHOTO_PICK), callback, "P:404").shouldBeNull()
    }

    should("save the biggest photo sent for the chosen cocktail") {
        every { photos.name(5) } returns "Негрони"
        every { photos.fileId(5) } returns "BIG"

        val result = admin().onMessage(context(AdminStep.PHOTO_UPLOAD, AdminState(cocktailId = 5)), photoMessage(size("small", 100), size("BIG", 900)))!!

        verify { photos.save(5, "BIG", 7L) }
        ((result.actions.first() as EditMessageAction).message.model as AdminPhotoUploadModel).saved shouldBe true
        result.actions.filterIsInstance<DeleteMessageIdAction>().single().messageId shouldBe 56
    }

    should("delete the photo of the chosen cocktail") {
        every { photos.name(5) } returns "Негрони"
        every { photos.fileId(5) } returns null

        val result = admin().onCallback(context(AdminStep.PHOTO_UPLOAD, AdminState(cocktailId = 5)), callback, "PD")!!

        verify { photos.delete(5) }
        ((result.actions.first() as EditMessageAction).message.model as AdminPhotoUploadModel).removed shouldBe true
    }

    should("ignore text while waiting for a photo and photos outside of the upload step") {
        admin().onMessage(context(AdminStep.PHOTO_UPLOAD, AdminState(cocktailId = 5)), textMessage("привет")).shouldBeNull()
        admin().onMessage(context(AdminStep.MAIN), photoMessage(size("x", 1))).shouldBeNull()
    }

    should("ignore messages and callbacks of a non-admin") {
        admin(isAdmin = false).onMessage(context(AdminStep.PHOTO_SEARCH), textMessage("негр")).shouldBeNull()
        admin(isAdmin = false).onCallback(context(AdminStep.MAIN), callback, "PH").shouldBeNull()
    }

    should("render the photo screens") {
        val renderer = FlowTemplateRenderer(FreemarkerConfig().freeMarkerConfigurer().also { it.afterPropertiesSet() })

        renderer.render(FlowKeys.ADMIN, "photo_search", ru, AdminPhotoSearchModel(3)) shouldContain "Загружено фото: <b>3</b>"
        renderer.render(FlowKeys.ADMIN, "photo_pick", ru, AdminPhotoPickModel("<x>", emptyList())) shouldContain "&lt;x&gt;"
        renderer.render(FlowKeys.ADMIN, "photo_upload", ru, AdminPhotoUploadModel(5, "Негрони", hasPhoto = false)) shouldContain "Пришлите фото"
        renderer.render(FlowKeys.ADMIN, "photo_upload", ru, AdminPhotoUploadModel(5, "Негрони", hasPhoto = true, saved = true)) shouldContain "Фото сохранено"
    }

    should("render the statistics and escape names") {
        val renderer = FlowTemplateRenderer(FreemarkerConfig().freeMarkerConfigurer().also { it.afterPropertiesSet() })
        val view = AdminView("15.06.2026 12:00", snapshot, "2.5", 25, "нед.")
        val text = renderer.render(FlowKeys.ADMIN, "main", ru, view)
        text shouldContain "Всего: <b>10</b>"
        text shouldContain "1. Водка &lt;b&gt; — 4"
        text shouldContain "Открытий карточек: 7"
        text shouldContain "(25%)"
    }
})
