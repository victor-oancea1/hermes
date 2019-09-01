package com.victor.oancea.hermes.application.util

import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.TimestampSynchronizer
import com.fasterxml.uuid.UUIDTimer
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.context.Context
import java.util.concurrent.ThreadLocalRandom


object UUID {
    private val UUIDGenerator = Generators.timeBasedGenerator(null, UUIDTimer(ThreadLocalRandom.current(), null as TimestampSynchronizer?))
    fun generate(): String = UUIDGenerator.generate().toString()
}

object ContextUtil {
    inline fun <T> withinFluxContext(crossinline f: (Context) -> Flux<T>): Flux<T> = Mono.subscriberContext().flatMapMany { f(it) }
    inline fun <T> withinMonoContext(crossinline f: (Context) -> Mono<T>): Mono<T> = Mono.subscriberContext().flatMap { f(it) }
}