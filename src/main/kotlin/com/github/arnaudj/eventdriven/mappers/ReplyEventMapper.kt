package com.github.arnaudj.eventdriven.mappers


interface ReplyEventMapper<in T, out O> {
    fun map(event: T, configMap: Map<String, Any>): O
}
