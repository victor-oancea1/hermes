package com.victor.oancea.hermes.domain

import reactor.core.publisher.Flux

data class EventMetadata(val topic: String,
                         val requestId: String? = null,
                         val partition: Int? = null,
                         val offset: Long? = null,
                         val timestamp: Long? = null,
                         val keySize: Int? = null,
                         val valueSize: Int? = null,
                         val headers: Map<String, String>? = null)

data class Event(val id: String?,
                 val key: String,
                 val value: Map<String, Any>,
                 val metadata: EventMetadata)

data class ReceiverOption(val offset: Offset,
                          val topic: String,
                          val expression: String,
                          val sourceFluxInspector: (Flux<Event>) -> Flux<Event> = { Flux.empty() },
                          val limit: Int)

enum class OffsetType {
    START,
    END,
    SPECIFIC
}

data class Offset(val offsetType: OffsetType, val value: Int? = null)
