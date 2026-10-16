package ru.grabovsky.drinkmebot.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.grabovsky.drinkmebot.repository.FlowStateRepository
import ru.grabovsky.drinkmebot.repository.UserDataRepository
import ru.grabovsky.drinkmebot.repository.UserRepository
import ru.grabovsky.drinkmebot.service.interfaces.DataLocaleService
import ru.grabovsky.drinkmebot.service.interfaces.UserDataService
import java.util.Locale

@Service
class UserDataServiceImpl(
    private val userRepository: UserRepository,
    private val flowStateRepository: FlowStateRepository,
    private val userData: UserDataRepository,
    private val dataLocale: DataLocaleService,
    private val objectMapper: ObjectMapper,
) : UserDataService {

    @Transactional(readOnly = true)
    override fun export(userId: Long, uiLocale: Locale): ByteArray {
        val user = userRepository.findUserByUserId(userId)
        val settings = user?.profile?.settings
        val lang = dataLocale.resolve(userId, uiLocale)
        val data = linkedMapOf(
            "user" to linkedMapOf(
                "id" to userId,
                "username" to user?.userName,
                "firstName" to user?.firstName,
                "lastName" to user?.lastName,
                "telegramLanguage" to user?.language,
                "createdAt" to user?.createdAt?.toString(),
                "lastActionAt" to user?.lastActionAt?.toString(),
            ),
            "settings" to linkedMapOf(
                "botLanguage" to settings?.cocktailLang,
                "autoDeleteMessages" to settings?.cleanChat,
            ),
            "ingredients" to userData.ingredientNames(userId, lang),
            "shoppingList" to userData.shoppingItems(userId, lang),
            "favoriteCocktails" to userData.markedCocktails(userId, lang, favorite = true),
            "cookedCocktails" to userData.markedCocktails(userId, lang, favorite = false),
            "activityDays" to userData.activityDays(userId).map { it.toString() },
        )
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(data)
    }

    @Transactional
    override fun deleteAll(userId: Long) {
        flowStateRepository.deleteByUserId(userId)
        // Профиль удаляется каскадом JPA, ингредиенты и дни активности - каскадом БД
        userRepository.findUserByUserId(userId)?.let(userRepository::delete)
        userRepository.flush()
    }
}
