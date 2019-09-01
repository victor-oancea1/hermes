package com.victor.oancea.hermes.web.handler

import com.victor.oancea.hermes.application.util.UUID
import com.victor.oancea.hermes.core.message.MessageReceiver
import com.victor.oancea.hermes.core.message.MessageSender
import com.victor.oancea.hermes.domain.Event
import com.victor.oancea.hermes.web.handler.QueryParamParser.toReceiverOptions
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType.APPLICATION_STREAM_JSON
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.context.Context

@RestController
class MessageHandler(private val messageSender: MessageSender, private val messageReceiver: MessageReceiver) {
    companion object {

        private val EVENT_LIST_TYPE = object : ParameterizedTypeReference<List<Event>>() {}
    }

    fun sendMessages(request: ServerRequest): Mono<ServerResponse> {
        val responseBody = request.bodyToMono(EVENT_LIST_TYPE)
                                  .flatMapMany { messageSender.send(it) }
                                  .subscriberContext(Context.of("REQUEST_ID", UUID.generate()))
        return ServerResponse
                .ok()
                .contentType(APPLICATION_STREAM_JSON)
                .body(responseBody, Event::class.java)
    }

    fun getMessages(request: ServerRequest): Mono<ServerResponse> {
        val receiverOptions = toReceiverOptions(request.queryParams())
        val responseBody = if (receiverOptions.isEmpty()) {
            Flux.empty()
        } else {
            messageReceiver
                    .receive(receiverOptions)
                    .subscriberContext(Context.of("REQUEST_ID", UUID.generate()))
        }

        return ServerResponse
                .ok()
                .contentType(APPLICATION_STREAM_JSON)
                .body(responseBody, Event::class.java)
    }
}