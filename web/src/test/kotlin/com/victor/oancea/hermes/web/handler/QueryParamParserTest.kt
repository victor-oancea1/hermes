package com.victor.oancea.hermes.web.handler

import com.victor.oancea.hermes.domain.Offset
import com.victor.oancea.hermes.domain.OffsetType.*
import com.victor.oancea.hermes.domain.ReceiverOption
import com.victor.oancea.hermes.web.handler.QueryParamParser.toReceiverOptions
import org.junit.jupiter.api.Test
import org.springframework.util.LinkedMultiValueMap

class QueryParamParserTest {

    @Test
    fun `receive event query param regex is valid`() {
        val receiverOptions = listOf(ReceiverOption(Offset(END), "some_topic_name", "true", limit = 10),
                                     ReceiverOption(Offset(START), "some_topic_name", "true", limit = 6),
                                     ReceiverOption(Offset(SPECIFIC, 38), "some_topic_name", "true", limit = 4),
                                     ReceiverOption(Offset(SPECIFIC, 11), "some_other_topic_name", "true", limit = 2),
                                     ReceiverOption(Offset(START), "some_other_topic_name", "true", limit = 33),
                                     ReceiverOption(Offset(END), "some_other_topic_name", "true", limit = 22))

        val queryParams = LinkedMultiValueMap<String, String>().also {
            it.add("bogus topic:some_topic_name|offset:end|limit:10 some other stuff", "true")
            it.add("topic:some_topic_name|offset:end|limit:10", "true")
            it.add("topic:some_topic_name|offset:start|limit:6", "true")
            it.add("topic:some_topic_name|offset:specific:38|limit:4", "true")
            it.add("topic:some_other_topic_name|offset:specific:11|limit:2", "true")
            it.add("topic:some_other_topic_name|offset:start|limit:33", "true")
            it.add("topic:some_other_topic_name|offset:end|limit:22", "true")
            it.add("topic:some_other_topic_name|offset:22|limit:22", "true")
            it.add("topic:some_other_topic_name|limit:22", "true")
            it.add("limit:22", "true")
            it.add("topic:bogus", "true")
            it.add("other bogus", "true")
            it.add("offset:end", "true")
            it.add("topic:some_other_topic_name|offset:end", "true")
        }

        assert(toReceiverOptions(queryParams) == receiverOptions)
    }

}