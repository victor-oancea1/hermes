package com.victor.oancea.hermes.web.handler

import com.victor.oancea.hermes.domain.Offset
import com.victor.oancea.hermes.domain.OffsetType
import com.victor.oancea.hermes.domain.ReceiverOption
import org.springframework.util.MultiValueMap
import java.util.stream.Stream
import kotlin.streams.toList

internal object QueryParamParser {
    private val RECEIVER_OPTION_REGEX = "topic:(\\w)+[|]offset:(end|start|(specific:\\d+))[|]limit:\\d+".toRegex()
    private val TOPIC_REGEX = "((topic):(\\w+))".toRegex()
    private val OFFSET_TYPE_SPECIFIC_REGEX = "((offset):((specific):(\\d+)))".toRegex()
    private val OFFSET_TYPE_REGEX = "((offset):(end|start))".toRegex()
    private val LIMIT_REGEX = "((limit):(\\d+))".toRegex()

    private fun isValid(receiverOption: String) = RECEIVER_OPTION_REGEX.matches(receiverOption)

    private fun buildReceiverOption(receiverOption: String, expression: String): ReceiverOption {
        val offsetInformation = getOffsetInformation(receiverOption)

        val offset = when (val offsetValue = offsetInformation.toIntOrNull()) {
            is Int -> Offset(OffsetType.SPECIFIC, offsetValue)
            else   -> Offset(OffsetType.valueOf(offsetInformation.toUpperCase()))
        }
        val limit = LIMIT_REGEX.find(receiverOption)!!.groupValues.last().toInt()

        val topic = TOPIC_REGEX.find(receiverOption)!!.groupValues.last()

        return ReceiverOption(offset, topic, expression, limit = limit)
    }

    private fun getOffsetInformation(receiverOption: String): String {
        return (OFFSET_TYPE_SPECIFIC_REGEX.find(receiverOption) ?: OFFSET_TYPE_REGEX.find(receiverOption)).let {
            it!!.groupValues.last()
        }
    }

    internal fun toReceiverOptions(queryParams: MultiValueMap<String, String>): List<ReceiverOption> {
        return queryParams
                .entries
                .stream()
                .flatMap { (key, values) -> if (isValid(key)) Stream.of(buildReceiverOption(key, values.single())) else Stream.empty() }
                .toList()
    }
}

