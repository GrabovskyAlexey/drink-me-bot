package ru.grabovsky.drinkmebot.strategy.flow.start

import org.springframework.stereotype.Component
import ru.grabovsky.drinkmebot.service.interfaces.I18nService
import ru.grabovsky.drinkmebot.strategy.flow.core.AbstractStaticFlow
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowKeys
import ru.grabovsky.drinkmebot.strategy.flow.core.engine.FlowStep
import ru.grabovsky.drinkmebot.strategy.flow.core.support.openButton

/** Приветствие с кнопками перехода к ингредиентам и коктейлям (сообщение перерисовывается в нужный экран). */
@Component
class StartFlow(
    i18n: I18nService,
) : AbstractStaticFlow(
    key = FlowKeys.START,
    step = StepKey.MAIN,
    bindingKey = MAIN_MESSAGE_BINDING,
    modelProvider = { context -> StartViewModel(context.user.userName ?: context.user.firstName) },
    buttonsProvider = { context ->
        listOf(
            FlowKeys.INGREDIENTS.openButton(i18n.i18n("buttons.start.ingredients", context.locale), row = 0),
            FlowKeys.COCKTAILS.openButton(i18n.i18n("buttons.start.cocktails", context.locale), row = 1),
        )
    },
) {
    companion object {
        private const val MAIN_MESSAGE_BINDING = "start_main"
    }
}

data class StartViewModel(
    val username: String
)

enum class StepKey(override val key: String) : FlowStep {
    MAIN("main"),
}
