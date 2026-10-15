package ru.grabovsky.drinkmebot.strategy.flow

import ru.grabovsky.drinkmebot.model.CocktailFilter
import ru.grabovsky.drinkmebot.model.CocktailTag
import ru.grabovsky.drinkmebot.service.interfaces.CocktailFilterService
import ru.grabovsky.drinkmebot.service.interfaces.DataLocaleService
import ru.grabovsky.drinkmebot.service.interfaces.I18nService
import ru.grabovsky.drinkmebot.util.DataLocale
import java.util.Locale

/** I18n для тестов flow: возвращает код ключа, с аргументами - `код(арг1,арг2)`. */
object EchoI18n : I18nService {
    override fun i18n(code: String, locale: Locale, default: String?, vararg args: Any?) =
        if (args.isEmpty()) code else "$code(${args.joinToString(",")})"
}

/** Фильтр коктейлей для тестов flow: хранит выбранные теги в памяти. */
class FakeCocktailFilterService : CocktailFilterService {
    private var tags = emptySet<CocktailTag>()
    private var strict = false
    override fun get(userId: Long) = CocktailFilter(tags, strict)
    override fun toggleStrict(userId: Long): Boolean {
        strict = !strict
        return strict
    }
    override fun toggle(userId: Long, tag: CocktailTag): CocktailFilter {
        tags = if (tag in tags) tags - tag else tags + tag
        return get(userId)
    }
    override fun reset(userId: Long) {
        tags = emptySet()
        strict = false
    }
}

/** Язык коктейлей для тестов flow: хранит явный выбор в памяти, иначе определяется по языку интерфейса. */
class FakeDataLocaleService(private var chosenLang: String? = null) : DataLocaleService {
    override fun resolve(userId: Long, uiLocale: Locale) = chosenLang ?: DataLocale.of(uiLocale)
    override fun chosen(userId: Long) = chosenLang
    override fun choose(userId: Long, lang: String?) {
        if (lang == null || lang in DataLocale.SUPPORTED) chosenLang = lang
    }
}
