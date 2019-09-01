package com.victor.oancea.hermes.message.service

import com.victor.oancea.hermes.application.event.EventReceiver
import com.victor.oancea.hermes.application.json.Extensions.deserializeToMap
import com.victor.oancea.hermes.domain.Event
import com.victor.oancea.hermes.domain.EventMetadata
import com.victor.oancea.hermes.domain.ReceiverOption
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverRecord

typealias StringReceiverRecord = ReceiverRecord<String, String>
typealias StringConsumerRecord = ConsumerRecord<String, String>

@Service
class KafkaEventReceiver(private val defaultReceiverFactory: (ReceiverOption) -> KafkaReceiver<String, String>) : EventReceiver {

    override fun receive(receiverOptions: List<ReceiverOption>): Flux<Event> {
        return Flux
                .fromIterable(receiverOptions)
                .map { receiverOption -> defaultReceiverFactory(receiverOption)
                                            .receive()
                                            .map { it.toEvent() }
                                            .let { receiverOption.sourceFluxInspector(it) } }
                .flatMap { it }
    }

    private fun <T : StringConsumerRecord> T.toEvent(): Event {
        val headers = headers().map { it.key() to String(it.value()!!) }.toMap()
        val metadata = EventMetadata(topic = topic(),
                                     partition = partition(),
                                     offset = offset(),
                                     timestamp = timestamp(),
                                     keySize = serializedKeySize(),
                                     valueSize = serializedValueSize(),
                                     headers = headers,
                                     requestId = headers["requestId"])
        return Event(metadata = metadata,
                     id = headers["id"],
                     key = key(),
                     value = value().deserializeToMap())
    }
}