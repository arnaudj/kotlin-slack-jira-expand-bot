package com.github.arnaudj.linkify.slackbot.dtos.replies

enum class JiraBotReplyMode {
    INLINE,
    THREAD;

    companion object {
        fun knownValues(): String = enumValues<JiraBotReplyMode>().joinToString { it.name.toLowerCase() }
    }
}
