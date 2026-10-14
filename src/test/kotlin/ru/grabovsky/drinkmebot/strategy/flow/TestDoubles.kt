package ru.grabovsky.drinkmebot.strategy.flow

import ru.grabovsky.drinkmebot.service.interfaces.DataLocaleService
import ru.grabovsky.drinkmebot.service.interfaces.I18nService
import ru.grabovsky.drinkmebot.util.DataLocale
import java.util.Locale

/** I18n для тестов flow: возвращает код ключа, с аргументами - `код(арг1,арг2)`. */
object EchoI18n : I18nService {
    override fun i18n(code: String, locale: Locale, default: String?, vararg args: Any?) =
        if (args.isEmpty()) code else "$code(${args.joinToString(",")})"
}

/** Язык коктейлей для тестов flow: хранит явный выбор в памяти, иначе определяется по языку интерфейса. */
class FakeDataLocaleService(private var chosenLang: String? = null) : DataLocaleService {
    override fun resolve(userId: Long, uiLocale: Locale) = chosenLang ?: DataLocale.of(uiLocale)
    override fun chosen(userId: Long) = chosenLang
    override fun choose(userId: Long, lang: String?) {
        if (lang == null || lang in DataLocale.SUPPORTED) chosenLang = lang
    }
}
