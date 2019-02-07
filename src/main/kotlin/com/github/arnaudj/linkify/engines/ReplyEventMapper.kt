package com.github.arnaudj.linkify.engines


interface ReplyEventMapper<in T, out O> {
    fun map(event: T, configMap: Map<String, Any>): O
}
