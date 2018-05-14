package com.github.arnaudj.linkify.slackbot.dtos.replies

enum class JiraBotReplyFormat {
    SHORT,
    EXTENDED;

    companion object {
        fun knownValues(): String = enumValues<JiraBotReplyFormat>().joinToString { it.name.toLowerCase() }
    }
}
