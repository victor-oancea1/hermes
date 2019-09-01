package com.victor.oancea.hermes.message.service

import com.victor.oancea.hermes.application.event.EventSender
import com.victor.oancea.hermes.application.extensions.plus
import com.victor.oancea.hermes.application.json.Extensions.serializeToString
import com.victor.oancea.hermes.application.util.ContextUtil.withinFluxContext
import com.victor.oancea.hermes.domain.Event
import com.victor.oancea.hermes.domain.EventMetadata
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.header.internals.RecordHeaders
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.toFlux
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderRecord
import reactor.kafka.sender.SenderResult
import java.util.stream.Collectors.collectingAndThen
import java.util.stream.Collectors.toMap
import java.util.stream.Collectors.toList as collectToList

typealias StringSenderRecord = SenderRecord<String, String, CorrelationMetadata>
typealias StringProducerRecord = ProducerRecord<String, String>

@Service
class KafkaEventSender(private val defaultSender: KafkaSender<String, String>) : EventSender {

    override fun send(events: List<Event>): Flux<Event> = withinFluxContext { context ->
        events
                .toFlux()
                .map { it.toStringSenderRecord(context["REQUEST_ID"]) }
                .let { defaultSender.send(it) }
                .let { it.map { result -> result.toPartialEvent() } }
                .let { it.mergeWith(events.toFlux()) }
                .let { it.collectList() }
                .let { it.flatMapMany(this::buildResultEventFlux) }
    }

    private fun buildResultEventFlux(allEvents: List<Event>): Flux<Event> {
        return allEvents
                .stream()
                .collect(toMap(Event::id,
                               { t -> t },
                               this::buildCompleteSentEvent))
                .let { it.values.toFlux() }
    }

    private fun buildCompleteSentEvent(event1: Event, event2: Event): Event {
        val sentEvent = if (event1.metadata.headers?.get("id")?.toBoolean() == true) event2 else event1
        val partialEvent = if (sentEvent == event1) event2 else event1

        return partialEvent.copy(metadata = partialEvent.metadata.copy(headers = sentEvent.metadata.headers + partialEvent.metadata.headers),
                                 key = sentEvent.key,
                                 value = sentEvent.value)
    }

    private fun SenderResult<CorrelationMetadata>.toPartialEvent(): Event {
        val requestId = correlationMetadata().requestId
        val id = correlationMetadata().eventId
        return Event(id = id,
                     key = "",
                     value = mapOf(),
                     metadata = EventMetadata(requestId = requestId,
                                              headers = mapOf("id" to id,
                                                              "requestId" to requestId),
                                              topic = recordMetadata()?.topic()!!,
                                              partition = recordMetadata()?.partition(),
                                              offset = recordMetadata()?.offset(),
                                              timestamp = recordMetadata()?.timestamp(),
                                              keySize = recordMetadata()?.serializedKeySize(),
                                              valueSize = recordMetadata()?.serializedValueSize()))
    }

    private fun Event.toStringSenderRecord(requestId: String): StringSenderRecord {
        return StringSenderRecord.create(StringProducerRecord(metadata.topic,
                                                              null,
                                                              System.currentTimeMillis(),
                                                              key,
                                                              value.serializeToString(),
                                                              metadata
                                                                      .headers
                                                                      .toHeaders()
                                                                      .add("requestId", requestId.toByteArray())
                                                                      .add("id", id!!.toByteArray())),
                                         CorrelationMetadata(id!!, requestId))
    }

    fun Map<String, String>?.toHeaders(): Headers = this?.entries
                                                            ?.stream()
                                                            ?.map { (k, v) -> RecordHeader(k, v.toByteArray()) }
                                                            ?.collect(collectingAndThen(collectToList()) { RecordHeaders(it) })
                                                    ?: RecordHeaders()
}

data class CorrelationMetadata(val eventId: String, val requestId: String)