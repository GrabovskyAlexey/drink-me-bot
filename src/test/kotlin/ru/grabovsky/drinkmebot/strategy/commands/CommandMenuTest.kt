package ru.grabovsky.drinkmebot.strategy.commands

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeUnique
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank

class CommandMenuTest : ShouldSpec({
    should("list the commands of the bot in menu order") {
        Command.entries.filter { !it.hidden }.sortedBy { it.order }.map { it.command } shouldContainExactly
            listOf("start", "ingredients", "cocktails", "shopping", "settings", "deleteme", "help")
    }

    should("have unique command names and orders") {
        Command.entries.map { it.command }.shouldBeUnique()
        Command.entries.map { it.order }.shouldBeUnique()
    }

    should("describe every command in both languages within the Telegram limit") {
        Command.entries.forEach {
            it.text.shouldNotBeBlank()
            it.textEn.shouldNotBeBlank()
            (it.text.length <= 256 && it.textEn.length <= 256) shouldBe true
            (it.text != it.textEn) shouldBe true
        }
    }
})
