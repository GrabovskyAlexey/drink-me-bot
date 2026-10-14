package ru.grabovsky.drinkmebot.entity

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant

/** Ингредиент, который есть у пользователя. */
@Entity
@Table(name = "user_ingredient", schema = "drink_me_bot")
data class UserIngredient(
    @EmbeddedId
    val id: UserIngredientId,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)

@Embeddable
data class UserIngredientId(
    @Column(name = "user_id")
    val userId: Long = 0,
    @Column(name = "goods_id")
    val goodsId: Int = 0,
) : Serializable
