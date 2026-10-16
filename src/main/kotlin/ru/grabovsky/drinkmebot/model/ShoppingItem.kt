package ru.grabovsky.drinkmebot.model

/** Позиция списка покупок: товар в локали пользователя и отметка «куплено». */
data class ShoppingItem(
    val goodsId: Int,
    val name: String,
    val bought: Boolean,
)
