package com.victor.oancea.hermes.application.extensions

import java.util.Collections.unmodifiableMap


operator fun <K, V> Map<K, V>?.plus(map: Map<K, V>?): Map<K, V>? {
    if (this == null && map == null) {
        return null
    }
    return unmodifiableMap((if (this == null || map == null) this ?: map else LinkedHashMap(this).apply { putAll(map) }))
}
