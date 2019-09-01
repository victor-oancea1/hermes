package com.victor.oancea.hermes.core.message

import com.victor.oancea.hermes.application.event.EventSender
import com.victor.oancea.hermes.application.util.UUID
import com.victor.oancea.hermes.domain.Event
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class MessageSender(private val eventSender: EventSender) {
    fun send(events: List<Event>): Flux<Event> = events
                                                    .map { it.copy(id = UUID.generate()) }
                                                    .let { eventSender.send(it) }
}