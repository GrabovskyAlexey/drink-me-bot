package ru.grabovsky.drinkmebot.strategy.flow.core.engine

interface FlowPayloadSerializer {
    fun <T : Any> deserialize(payload: String?, type: Class<T>): T
    fun <T : Any> serialize(payload: T?): String?
}
