package ru.grabovsky.drinkmebot.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import ru.grabovsky.drinkmebot.entity.UserIngredient
import ru.grabovsky.drinkmebot.entity.UserIngredientId

@Repository
interface UserIngredientRepository : JpaRepository<UserIngredient, UserIngredientId> {
    @Modifying
    @Query("delete from UserIngredient u where u.id.userId = :userId")
    fun deleteAllByUserId(@Param("userId") userId: Long): Int

    @Query("select count(u) from UserIngredient u where u.id.userId = :userId")
    fun countByUserId(@Param("userId") userId: Long): Long
}
