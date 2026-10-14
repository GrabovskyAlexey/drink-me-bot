package ru.grabovsky.drinkmebot.service

import org.springframework.stereotype.Service
import ru.grabovsky.drinkmebot.service.interfaces.DataLocaleService
import ru.grabovsky.drinkmebot.service.interfaces.UserService
import ru.grabovsky.drinkmebot.util.DataLocale
import java.util.Locale

@Service
class DataLocaleServiceImpl(
    private val userService: UserService,
) : DataLocaleService {

    override fun resolve(userId: Long, uiLocale: Locale): String = chosen(userId) ?: DataLocale.of(uiLocale)

    override fun chosen(userId: Long): String? =
        userService.getUser(userId)?.profile?.settings?.cocktailLang?.takeIf { it in DataLocale.SUPPORTED }

    override fun choose(userId: Long, lang: String?) {
        if (lang != null && lang !in DataLocale.SUPPORTED) return
        val user = userService.getUser(userId) ?: return
        val profile = user.profile ?: return
        profile.settings.cocktailLang = lang
        userService.saveUser(user)
    }
}
