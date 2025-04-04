package com.victor.oancea.hermes.message.service

import com.victor.oancea.hermes.domain.Event
import com.victor.oancea.hermes.domain.EventMetadata
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderRecord
import reactor.kafka.sender.SenderResult
import reactor.test.StepVerifier
import java.time.Duration

class KafkaEventSenderTest {

    private lateinit var kafkaEventSender: KafkaEventSender
    private lateinit var testKafkaSender: TestKafkaSender

    @BeforeEach
    fun setUp() {
        testKafkaSender = TestKafkaSender()
        kafkaEventSender = KafkaEventSender(testKafkaSender)
    }

    @Test
    fun `should send events to Kafka`() {
        // Given
        val topic = "test-topic"
        val eventId = "event123"
        val requestId = "req123"
        
        val event = Event(
            id = eventId,
            key = "key1",
            value = mapOf("field1" to "value1", "field2" to 42),
            metadata = EventMetadata(
                topic = topic,
                requestId = requestId,
                headers = mapOf("id" to eventId, "requestId" to requestId)
            )
        )
        
        val recordMetadata = RecordMetadata(
            TopicPartition(topic, 0),
            0L,
            0L,
            System.currentTimeMillis(),
            0L,
            10,
            20
        )
        
        // Configure test sender to return a specific result
        testKafkaSender.nextResult = Flux.just(
            TestSenderResult(
                CorrelationMetadata(eventId, requestId),
                recordMetadata
            )
        )
        
        // When
        val result = kafkaEventSender.send(listOf(event))
        
        // Then
        StepVerifier.create(result)
            .expectNextMatches { sentEvent ->
                sentEvent.id == eventId &&
                sentEvent.metadata.topic == topic &&
                sentEvent.metadata.headers?.get("id") == eventId &&
                sentEvent.metadata.headers?.get("requestId") == requestId
            }
            .expectComplete()
            .verify(Duration.ofSeconds(5))
    }
    
    @Test
    fun `should handle multiple events`() {
        // Given
        val topic = "test-topic"
        val eventId1 = "event123"
        val eventId2 = "event456"
        val requestId = "req123"
        
        val event1 = Event(
            id = eventId1,
            key = "key1",
            value = mapOf("field1" to "value1"),
            metadata = EventMetadata(
                topic = topic,
                requestId = requestId,
                headers = mapOf("id" to eventId1, "requestId" to requestId)
            )
        )
        
        val event2 = Event(
            id = eventId2,
            key = "key2",
            value = mapOf("field2" to "value2"),
            metadata = EventMetadata(
                topic = topic,
                requestId = requestId,
                headers = mapOf("id" to eventId2, "requestId" to requestId)
            )
        )
        
        val recordMetadata1 = RecordMetadata(
            TopicPartition(topic, 0),
            0L,
            0L,
            System.currentTimeMillis(),
            0L,
            10,
            20
        )
        
        val recordMetadata2 = RecordMetadata(
            TopicPartition(topic, 1),
            0L,
            10L,
            System.currentTimeMillis(),
            0L,
            15,
            25
        )
        
        // Configure test sender to return specific results
        testKafkaSender.nextResult = Flux.just(
            TestSenderResult(
                CorrelationMetadata(eventId1, requestId),
                recordMetadata1
            ),
            TestSenderResult(
                CorrelationMetadata(eventId2, requestId),
                recordMetadata2
            )
        )
        
        // When
        val result = kafkaEventSender.send(listOf(event1, event2))
        
        // Then
        StepVerifier.create(result)
            .expectNextCount(2)
            .expectComplete()
            .verify(Duration.ofSeconds(5))
    }
    
    @Test
    fun `should handle empty event list`() {
        // Given
        testKafkaSender.nextResult = Flux.empty()
        
        // When
        val result = kafkaEventSender.send(emptyList())
        
        // Then
        StepVerifier.create(result)
            .expectComplete()
            .verify(Duration.ofSeconds(5))
    }
    
    // Test implementation of KafkaSender that avoids type inference issues
    class TestKafkaSender : KafkaSender<String, String> {
        var nextResult: Flux<SenderResult<CorrelationMetadata>> = Flux.empty()
        
        override fun <T : Any> send(publisher: Publisher<out SenderRecord<String, String, T>>): Flux<SenderResult<T>> {
            @Suppress("UNCHECKED_CAST")
            return nextResult as Flux<SenderResult<T>>
        }
        
        override fun <T : Any> doOnProducer(function: java.util.function.Function<org.apache.kafka.clients.producer.Producer<String, String>, out T>): reactor.core.publisher.Mono<T> {
            throw UnsupportedOperationException("Not implemented for tests")
        }
        
        override fun createOutbound(): reactor.kafka.sender.KafkaOutbound<String, String> {
            throw UnsupportedOperationException("Not implemented for tests")
        }
        
        override fun <T : Any> sendTransactionally(publisher: Publisher<out Publisher<out SenderRecord<String, String, T>>>): Flux<Flux<SenderResult<T>>> {
            throw UnsupportedOperationException("Not implemented for tests")
        }
        
        override fun transactionManager(): reactor.kafka.sender.TransactionManager {
            throw UnsupportedOperationException("Not implemented for tests")
        }
        
        override fun close() {}
    }
    
    // Simple implementation of SenderResult for testing
    class TestSenderResult(
        private val correlationMetadata: CorrelationMetadata,
        private val metadata: RecordMetadata
    ) : SenderResult<CorrelationMetadata> {
        override fun correlationMetadata(): CorrelationMetadata = correlationMetadata
        override fun recordMetadata(): RecordMetadata = metadata
        override fun exception(): Exception? = null
    }
}
