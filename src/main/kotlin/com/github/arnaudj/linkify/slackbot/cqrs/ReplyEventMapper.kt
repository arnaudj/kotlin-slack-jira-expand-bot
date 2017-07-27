package com.github.arnaudj.linkify.slackbot.cqrs


interface ReplyEventMapper<in T, out O> {
    fun map(event: T): O
}
