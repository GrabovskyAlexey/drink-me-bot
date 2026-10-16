package ru.grabovsky.drinkmebot.config

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import ru.grabovsky.drinkmebot.model.IngredientCategory
import ru.grabovsky.drinkmebot.util.DataLocale
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

private val INTERFACE_LANGUAGES = listOf("ru", "en", "uk", "de", "es", "fr", "it")

/** Защищает от опечаток в локализации: пропавшие ключи, расхождение языков, склеенные строки. */
class MessagesConsistencyTest : ShouldSpec({
    fun load(name: String): Properties = Properties().apply {
        MessagesConsistencyTest::class.java.getResourceAsStream("/$name")!!.reader(StandardCharsets.UTF_8).use { load(it) }
    }

    val ru = load("messages_ru.properties")
    val en = load("messages_en.properties")
    val default = load("messages.properties")

    should("have identical key sets in ru, en and the default bundle") {
        (ru.stringPropertyNames() - en.stringPropertyNames()).shouldBeEmpty()
        (en.stringPropertyNames() - ru.stringPropertyNames()).shouldBeEmpty()
        (default.stringPropertyNames() - en.stringPropertyNames()).shouldBeEmpty()
        (en.stringPropertyNames() - default.stringPropertyNames()).shouldBeEmpty()
    }

    should("translate every key into every interface language with the same placeholders") {
        val placeholders = Regex("""\{\d+}""")
        val problems = INTERFACE_LANGUAGES.flatMap { language ->
            val props = load("messages_$language.properties")
            val missing = (default.stringPropertyNames() - props.stringPropertyNames()).map { "$language: missing $it" }
            val extra = (props.stringPropertyNames() - default.stringPropertyNames()).map { "$language: extra $it" }
            val broken = default.stringPropertyNames().filter { it in props.stringPropertyNames() }.filter {
                placeholders.findAll(default.getProperty(it)).map { m -> m.value }.toSet() !=
                    placeholders.findAll(props.getProperty(it)).map { m -> m.value }.toSet()
            }.map { "$language: placeholders of $it" }
            val apostrophes = props.stringPropertyNames()
                .filter { '\'' in props.getProperty(it) && placeholders.containsMatchIn(props.getProperty(it)) }
                .map { "$language: apostrophe in $it" }
            missing + extra + broken + apostrophes
        }
        problems.shouldBeEmpty()
    }

    should("have a template for every step in every interface language") {
        val templates = Files.walk(Path.of("src/main/resources/message/template")).use { stream ->
            stream.filter { it.toString().endsWith("_ru.ftl") }.toList()
        }
        val missing = templates.flatMap { ru ->
            INTERFACE_LANGUAGES.map { language -> Path.of(ru.toString().removeSuffix("_ru.ftl") + "_$language.ftl") }
        }.filter { !Files.exists(it) }
        missing.shouldBeEmpty()
    }

    should("not contain several keys glued into one value") {
        val glued = listOf(ru, en).flatMap { props ->
            props.stringPropertyNames().filter { Regex("""(buttons|alerts|unit|notices)\.[a-z_.]+=""").containsMatchIn(props.getProperty(it)) }
        }
        glued.shouldBeEmpty()
    }

    should("define a label for every ingredient category") {
        val missing = IngredientCategory.entries.map { it.messageKey }.filter { it !in ru.stringPropertyNames() }
        missing.shouldBeEmpty()
    }

    should("define the keys that flows build dynamically") {
        val dynamic = listOf("ready", "missing1", "missing2").flatMap { listOf("buttons.cocktails.$it", "cocktails.section.$it") }
        val languages = DataLocale.SUPPORTED.map { "settings.language.$it" }
        ((dynamic + languages + listOf("buttons.settings.clean.on", "buttons.settings.clean.off")) - ru.stringPropertyNames()).shouldBeEmpty()
    }

    should("define every message key used in the source code") {
        val keyPattern = Regex(""""((?:buttons|alerts|unit|notices)\.[a-z_]+(?:\.[a-z_]+)*)"""")
        val used = Files.walk(Path.of("src/main/kotlin")).use { stream ->
            stream.filter { it.toString().endsWith(".kt") }.toList()
        }.flatMap { file -> keyPattern.findAll(Files.readString(file)).map { it.groupValues[1] }.toList() }.toSet()

        (used - ru.stringPropertyNames()).shouldBeEmpty()
    }
})
