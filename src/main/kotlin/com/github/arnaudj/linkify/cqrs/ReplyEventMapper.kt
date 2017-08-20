package com.github.arnaudj.linkify.cqrs


interface ReplyEventMapper<in T, out O> {
    fun map(event: T, configMap: Map<String, Any>): O
}
