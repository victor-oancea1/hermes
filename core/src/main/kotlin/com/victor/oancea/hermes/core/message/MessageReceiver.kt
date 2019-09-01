package com.victor.oancea.hermes.core.message

import com.victor.oancea.hermes.application.event.EventReceiver
import com.victor.oancea.hermes.application.json.Extensions.serializeToMap
import com.victor.oancea.hermes.domain.Event
import com.victor.oancea.hermes.domain.ReceiverOption
import org.springframework.context.expression.MapAccessor
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class MessageReceiver(private val eventReceiver: EventReceiver) {

    private final val expressionParser = SpelExpressionParser()

    fun receive(receiverOptions: List<ReceiverOption>): Flux<Event> {
        return receiverOptions
                .map(this::addInspector)
                .let { eventReceiver.receive(it) }
    }

    private fun addInspector(receiverOption: ReceiverOption) = receiverOption.copy(sourceFluxInspector = { sourceFlux ->
        sourceFlux
                .filter { it.matches(receiverOption.expression) }
                .take(receiverOption.limit.toLong())
    })

    private fun Event.matches(stringExpression: String): Boolean {
        val eventAsMap = serializeToMap()
        val context = StandardEvaluationContext(eventAsMap).also { it.addPropertyAccessor(MapAccessor()) }

        val expression = expressionParser.parseExpression(stringExpression)

        return expression.getValue(context, eventAsMap, Boolean::class.java)!!
    }
}
