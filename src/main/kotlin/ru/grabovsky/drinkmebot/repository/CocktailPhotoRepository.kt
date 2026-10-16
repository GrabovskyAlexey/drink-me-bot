package ru.grabovsky.drinkmebot.repository

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import ru.grabovsky.drinkmebot.model.CocktailSearchItem

/** Фото коктейлей (`cocktail_photo`) и поиск коктейлей для администратора. */
@Repository
class CocktailPhotoRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) {

    fun fileId(cocktailId: Int): String? =
        jdbc.queryForList(
            "SELECT file_id FROM drink_me_bot.cocktail_photo WHERE cocktail_id = :id",
            MapSqlParameterSource("id", cocktailId), String::class.java,
        ).firstOrNull()

    fun save(cocktailId: Int, fileId: String, uploadedBy: Long) {
        jdbc.update(
            """
            INSERT INTO drink_me_bot.cocktail_photo (cocktail_id, file_id, uploaded_by)
            VALUES (:id, :fileId, :by)
            ON CONFLICT (cocktail_id) DO UPDATE SET file_id = :fileId, uploaded_by = :by, uploaded_at = NOW()
            """.trimIndent(),
            MapSqlParameterSource("id", cocktailId).addValue("fileId", fileId).addValue("by", uploadedBy),
        )
    }

    fun delete(cocktailId: Int): Boolean =
        jdbc.update("DELETE FROM drink_me_bot.cocktail_photo WHERE cocktail_id = :id", MapSqlParameterSource("id", cocktailId)) > 0

    fun count(): Int = jdbc.queryForObject("SELECT count(*) FROM drink_me_bot.cocktail_photo", MapSqlParameterSource(), Int::class.java) ?: 0

    /** Русское название коктейля по id (каталог `ru` содержит все коктейли). */
    fun name(cocktailId: Int): String? =
        jdbc.queryForList(
            "SELECT name FROM drink_me_bot.cocktail_i18n WHERE cocktail_id = :id AND lang = 'ru'",
            MapSqlParameterSource("id", cocktailId), String::class.java,
        ).firstOrNull()

    /** Поиск по части русского названия без учёта регистра (не больше [limit]); число в запросе - ещё и поиск по id. */
    fun search(query: String, limit: Int): List<CocktailSearchItem> =
        jdbc.query(
            """
            SELECT c.cocktail_id, c.name, (p.cocktail_id IS NOT NULL) AS has_photo
            FROM drink_me_bot.cocktail_i18n c
            LEFT JOIN drink_me_bot.cocktail_photo p ON p.cocktail_id = c.cocktail_id
            WHERE c.lang = 'ru' AND (lower(c.name) LIKE :pattern ESCAPE '\' OR c.cocktail_id = :id)
            ORDER BY c.name, c.cocktail_id
            LIMIT :limit
            """.trimIndent(),
            MapSqlParameterSource("pattern", "%${escapeLike(query.lowercase())}%")
                .addValue("id", query.toIntOrNull() ?: -1).addValue("limit", limit),
        ) { rs, _ -> CocktailSearchItem(rs.getInt("cocktail_id"), rs.getString("name"), rs.getBoolean("has_photo")) }

    private fun escapeLike(value: String) = value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
