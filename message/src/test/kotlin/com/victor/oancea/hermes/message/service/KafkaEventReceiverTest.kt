package com.victor.oancea.hermes.message.service

import com.victor.oancea.hermes.domain.Event
import com.victor.oancea.hermes.domain.EventMetadata
import com.victor.oancea.hermes.domain.Offset
import com.victor.oancea.hermes.domain.OffsetType
import com.victor.oancea.hermes.domain.ReceiverOption
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.header.internals.RecordHeaders
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOffset
import reactor.kafka.receiver.ReceiverRecord
import reactor.kafka.sender.KafkaOutbound
import reactor.kafka.sender.TransactionManager
import reactor.test.StepVerifier
import java.time.Duration

class KafkaEventReceiverTest {

    @Test
    fun `should receive events from Kafka`() {
        // Given
        val topic = "test-topic"
        
        val headers = RecordHeaders()
        headers.add(RecordHeader("requestId", "req123".toByteArray()))
        headers.add(RecordHeader("id", "event123".toByteArray()))
        
        val mockReceiverOffset = mockk<ReceiverOffset>()
        every { mockReceiverOffset.acknowledge() } returns Unit
        
        val receiverRecord = ReceiverRecord(
            ConsumerRecord(
                topic,
                0,
                0L,
                "key1",
                "{\"field1\":\"value1\",\"field2\":42}"
            ),
            mockReceiverOffset
        )
        
        val mockKafkaReceiver = mockk<KafkaReceiver<String, String>>()
        every { mockKafkaReceiver.receive() } returns Flux.just(receiverRecord)
        
        val mockReceiverFactory: (ReceiverOption) -> KafkaReceiver<String, String> = { _ -> mockKafkaReceiver }
        
        val kafkaEventReceiver = KafkaEventReceiver(mockReceiverFactory)
        
        val receiverOption = ReceiverOption(
            offset = Offset(OffsetType.START),
            topic = topic,
            expression = "*",
            limit = 10,
            sourceFluxInspector = { it }
        )
        
        // When
        val result = kafkaEventReceiver.receive(listOf(receiverOption))
        
        // Then
        StepVerifier.create(result)
            .expectNextMatches { event ->
                event.key == "key1" &&
                event.metadata.topic == topic &&
                event.metadata.partition == 0 &&
                event.metadata.offset == 0L &&
                event.value["field1"] == "value1" &&
                event.value["field2"] == 42
            }
            .expectComplete()
            .verify(Duration.ofSeconds(5))
        
        verify { mockKafkaReceiver.receive() }
    }
    
    @Test
    fun `should handle multiple receiver options`() {
        // Given
        val topic1 = "test-topic-1"
        val topic2 = "test-topic-2"
        
        val headers1 = RecordHeaders()
        headers1.add(RecordHeader("requestId", "req123".toByteArray()))
        headers1.add(RecordHeader("id", "event123".toByteArray()))
        
        val headers2 = RecordHeaders()
        headers2.add(RecordHeader("requestId", "req456".toByteArray()))
        headers2.add(RecordHeader("id", "event456".toByteArray()))
        
        val mockReceiverOffset1 = mockk<ReceiverOffset>()
        val mockReceiverOffset2 = mockk<ReceiverOffset>()
        every { mockReceiverOffset1.acknowledge() } returns Unit
        every { mockReceiverOffset2.acknowledge() } returns Unit
        
        val receiverRecord1 = ReceiverRecord(
            ConsumerRecord(
                topic1,
                0,
                0L,
                "key1",
                "{\"field1\":\"value1\"}"
            ),
            mockReceiverOffset1
        )
        
        val receiverRecord2 = ReceiverRecord(
            ConsumerRecord(
                topic2,
                1,
                10L,
                "key2",
                "{\"field2\":\"value2\"}"
            ),
            mockReceiverOffset2
        )
        
        val mockKafkaReceiver1 = mockk<KafkaReceiver<String, String>>()
        val mockKafkaReceiver2 = mockk<KafkaReceiver<String, String>>()
        
        every { mockKafkaReceiver1.receive() } returns Flux.just(receiverRecord1)
        every { mockKafkaReceiver2.receive() } returns Flux.just(receiverRecord2)
        
        val mockReceiverFactory: (ReceiverOption) -> KafkaReceiver<String, String> = { option ->
            if (option.topic == topic1) mockKafkaReceiver1 else mockKafkaReceiver2
        }
        
        val kafkaEventReceiver = KafkaEventReceiver(mockReceiverFactory)
        
        val receiverOption1 = ReceiverOption(
            offset = Offset(OffsetType.START),
            topic = topic1,
            expression = "*",
            limit = 10,
            sourceFluxInspector = { it }
        )
        
        val receiverOption2 = ReceiverOption(
            offset = Offset(OffsetType.START),
            topic = topic2,
            expression = "*",
            limit = 10,
            sourceFluxInspector = { it }
        )
        
        // When
        val result = kafkaEventReceiver.receive(listOf(receiverOption1, receiverOption2))
        
        // Then
        StepVerifier.create(result)
            .expectNextMatches { event ->
                event.key == "key1" &&
                event.metadata.topic == topic1 &&
                event.value["field1"] == "value1"
            }
            .expectNextMatches { event ->
                event.key == "key2" &&
                event.metadata.topic == topic2 &&
                event.value["field2"] == "value2"
            }
            .expectComplete()
            .verify(Duration.ofSeconds(5))
        
        verify { mockKafkaReceiver1.receive() }
        verify { mockKafkaReceiver2.receive() }
    }
}
