package com.github.arnaudj.linkify.slackbot.cqrs.mappers

enum class JiraBotReplyFormat {
    SHORT,
    EXTENDED;

    companion object {
        fun knownValues(): String = enumValues<JiraBotReplyFormat>().joinToString { it.name.toLowerCase() }
    }
}
