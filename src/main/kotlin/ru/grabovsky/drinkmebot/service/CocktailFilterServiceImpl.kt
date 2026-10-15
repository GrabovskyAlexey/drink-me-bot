package ru.grabovsky.drinkmebot.service

import org.springframework.stereotype.Service
import ru.grabovsky.drinkmebot.model.CocktailFilter
import ru.grabovsky.drinkmebot.model.CocktailTag
import ru.grabovsky.drinkmebot.service.interfaces.CocktailFilterService
import ru.grabovsky.drinkmebot.service.interfaces.UserService

@Service
class CocktailFilterServiceImpl(
    private val userService: UserService,
) : CocktailFilterService {

    override fun get(userId: Long): CocktailFilter =
        CocktailFilter(userService.getUser(userId)?.profile?.settings?.cocktailTags.orEmpty().toTags())

    override fun toggle(userId: Long, tag: CocktailTag): CocktailFilter {
        val current = get(userId).tags
        return save(userId, if (tag in current) current - tag else current + tag)
    }

    override fun reset(userId: Long) {
        save(userId, emptySet())
    }

    private fun save(userId: Long, tags: Set<CocktailTag>): CocktailFilter {
        val user = userService.getUser(userId) ?: return CocktailFilter(tags)
        val profile = user.profile ?: return CocktailFilter(tags)
        profile.settings.cocktailTags = tags.map { it.name }.toSet()
        userService.saveUser(user)
        return CocktailFilter(tags)
    }

    /** Неизвестные имена (тег убрали из кода) пропускаем. */
    private fun Set<String>.toTags(): Set<CocktailTag> = mapNotNull { name -> CocktailTag.entries.find { it.name == name } }.toSet()
}
