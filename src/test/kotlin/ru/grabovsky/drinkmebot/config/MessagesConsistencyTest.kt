package ru.grabovsky.drinkmebot.config

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import ru.grabovsky.drinkmebot.model.IngredientCategory
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

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

    should("define every message key used in the source code") {
        val keyPattern = Regex(""""((?:buttons|alerts|unit|notices)\.[a-z_]+(?:\.[a-z_]+)*)"""")
        val used = Files.walk(Path.of("src/main/kotlin")).use { stream ->
            stream.filter { it.toString().endsWith(".kt") }.toList()
        }.flatMap { file -> keyPattern.findAll(Files.readString(file)).map { it.groupValues[1] }.toList() }.toSet()

        (used - ru.stringPropertyNames()).shouldBeEmpty()
    }
})
