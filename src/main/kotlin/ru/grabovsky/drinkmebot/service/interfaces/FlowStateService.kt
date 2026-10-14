package ru.grabovsky.drinkmebot.service.interfaces

import ru.grabovsky.drinkmebot.entity.FlowState
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowKey
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowStateSnapshot

interface FlowStateService {
    fun load(userId: Long, flowKey: FlowKey): FlowStateSnapshot?
    fun save(snapshot: FlowStateSnapshot)
    fun clear(userId: Long, flowKey: FlowKey)
    fun findListFlow(userId: Long): FlowState?
}