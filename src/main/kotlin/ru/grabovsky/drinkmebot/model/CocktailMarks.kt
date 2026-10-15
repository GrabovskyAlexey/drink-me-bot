package ru.grabovsky.drinkmebot.model

/** Отметки пользователя на коктейле: в избранном и «уже готовил». */
data class CocktailMarks(val favorite: Boolean = false, val cooked: Boolean = false)
