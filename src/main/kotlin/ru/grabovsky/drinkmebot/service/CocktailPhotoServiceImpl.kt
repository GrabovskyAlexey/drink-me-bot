package ru.grabovsky.drinkmebot.service

import org.springframework.stereotype.Service
import ru.grabovsky.drinkmebot.model.CocktailSearchItem
import ru.grabovsky.drinkmebot.repository.CocktailPhotoRepository
import ru.grabovsky.drinkmebot.service.interfaces.CocktailPhotoService
import ru.grabovsky.drinkmebot.service.interfaces.CocktailPhotoService.Companion.MAX_QUERY_LENGTH
import ru.grabovsky.drinkmebot.service.interfaces.CocktailPhotoService.Companion.SEARCH_LIMIT

@Service
class CocktailPhotoServiceImpl(
    private val repository: CocktailPhotoRepository,
) : CocktailPhotoService {
    override fun fileId(cocktailId: Int): String? = repository.fileId(cocktailId)

    override fun save(cocktailId: Int, fileId: String, uploadedBy: Long) = repository.save(cocktailId, fileId, uploadedBy)

    override fun delete(cocktailId: Int): Boolean = repository.delete(cocktailId)

    override fun count(): Int = repository.count()

    override fun name(cocktailId: Int): String? = repository.name(cocktailId)

    override fun search(query: String): List<CocktailSearchItem> {
        val normalized = query.trim().take(MAX_QUERY_LENGTH)
        if (normalized.isEmpty()) return emptyList()
        return repository.search(normalized, SEARCH_LIMIT)
    }
}
