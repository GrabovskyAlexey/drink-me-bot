package ru.grabovsky.drinkmebot.repository

/** Запасной язык каталога: коктейли, которых нет на языке пользователя, показываются на английском (`us`). */
const val FALLBACK_LANG = "us"

/**
 * Фрагмент `WITH`: для каждого коктейля язык показа - язык пользователя (параметр `:lang`), если коктейль на нём есть,
 * иначе [FALLBACK_LANG]. Название, состав, теги и шаги коктейля берутся именно на этом языке.
 */
const val COCKTAIL_LANG_CTE = """
    cocktail_lang AS (
        SELECT c.id AS cocktail_id,
               CASE WHEN EXISTS (SELECT 1 FROM drink_me_bot.cocktail_i18n i WHERE i.cocktail_id = c.id AND i.lang = :lang)
                    THEN :lang ELSE '$FALLBACK_LANG' END AS lang
        FROM drink_me_bot.cocktail c
    )
"""
