package ru.grabovsky.drinkmebot.strategy.commands

import org.springframework.stereotype.Component
import ru.grabovsky.drinkmebot.service.interfaces.UserService
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowEngine
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowKeys

@Component
class DeleteMeCommand(
    userService: UserService,
    flowEngine: FlowEngine,
) : AbstractCommand(Command.DELETE_ME, FlowKeys.DELETE_ME, userService, flowEngine)
