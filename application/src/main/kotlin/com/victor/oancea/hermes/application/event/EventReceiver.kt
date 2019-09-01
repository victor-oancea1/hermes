package com.victor.oancea.hermes.application.event

import com.victor.oancea.hermes.domain.Event
import com.victor.oancea.hermes.domain.ReceiverOption
import reactor.core.publisher.Flux

interface EventReceiver {
    fun receive(receiverOptions: List<ReceiverOption>): Flux<Event>
}