package com.victor.oancea.hermes.web.route

import com.victor.oancea.hermes.web.handler.MessageHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_STREAM_JSON
import org.springframework.web.reactive.function.server.router


@Configuration
class RouteConfiguration {

    @Bean
    fun messageRouter(messageHandler: MessageHandler) = router {
        "/hermes/api/v1/messages".nest {
            accept(APPLICATION_STREAM_JSON).nest {
                GET("/", messageHandler::getMessages)
            }
            (accept(APPLICATION_STREAM_JSON) and contentType(APPLICATION_JSON)).nest {
                POST("/", messageHandler::sendMessages)
            }
        }
    }

}