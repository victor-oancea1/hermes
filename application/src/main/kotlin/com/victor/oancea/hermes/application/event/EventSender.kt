package com.victor.oancea.hermes.application.event

import com.victor.oancea.hermes.domain.Event
import reactor.core.publisher.Flux

interface EventSender {
    fun send(events: List<Event>): Flux<Event>
}