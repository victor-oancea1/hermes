package com.victor.oancea.hermes.message.configuration

import com.victor.oancea.hermes.domain.OffsetType.*
import com.victor.oancea.hermes.domain.ReceiverOption
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverPartition
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import java.time.Duration

@Configuration
class KafkaConfiguration {
    companion object {
        private const val CLIENT_ID = "hermes"
        private const val GROUP_ID = "hermes"

        private fun buildDefaultReceiverOptions(kafkaServers: String): ReceiverOptions<String, String> {
            return ReceiverOptions
                    .create<String, String>(mapOf())
                    .withKeyDeserializer(StringDeserializer())
                    .withValueDeserializer(StringDeserializer())
                    .consumerProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers)
                    .consumerProperty(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID)
                    .consumerProperty(ConsumerConfig.CLIENT_ID_CONFIG, CLIENT_ID)
                    .consumerProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
                    .consumerProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
                    .commitInterval(Duration.ZERO)
                    .commitBatchSize(0)
        }

        private fun buildDefaultSenderOptions(kafkaServers: String): SenderOptions<String, String> {
            return SenderOptions
                    .create<String, String>(mapOf())
                    .withValueSerializer(StringSerializer())
                    .withKeySerializer(StringSerializer())
                    .producerProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers)
                    .producerProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
                    .producerProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
                    .producerProperty(ProducerConfig.CLIENT_ID_CONFIG, CLIENT_ID)
                    .producerProperty(ProducerConfig.ACKS_CONFIG, "all")
                    .maxInFlight(1024)
        }

        private fun ReceiverOption.buildReceiver(kafkaServers: String): KafkaReceiver<String, String> {
            return buildDefaultReceiverOptions(kafkaServers)
                    .addAssignListener { changeReceiveIndex(it) }
                    .subscription(listOf(topic))
                    .let { KafkaReceiver.create(it) }
        }

        private fun ReceiverOption.changeReceiveIndex(partitions: Iterable<ReceiverPartition>) = partitions.forEach {
            when (offset.offsetType) {
                START    -> it.seekToBeginning()
                END      -> it.seekToEnd()
                SPECIFIC -> it.seek(offset.value!!.toLong())
            }
        }
    }

    @Bean
    fun defaultReceiverFactory(@Value("\${KAFKA_BOOTSTRAP_SERVERS}") kafkaServers: String): (ReceiverOption) -> KafkaReceiver<String, String> {
        return { it.buildReceiver(kafkaServers) }
    }

    @Bean(destroyMethod = "close")
    fun defaultSender(@Value("\${KAFKA_BOOTSTRAP_SERVERS}") kafkaServers: String): KafkaSender<String, String> {
        return KafkaSender.create(buildDefaultSenderOptions(kafkaServers))
    }
}



