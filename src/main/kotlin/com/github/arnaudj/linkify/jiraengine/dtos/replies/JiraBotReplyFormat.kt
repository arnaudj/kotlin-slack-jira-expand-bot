package com.github.arnaudj.linkify.jiraengine.dtos.replies

enum class JiraBotReplyFormat {
    SHORT,
    EXTENDED;

    companion object {
        fun knownValues(): String = enumValues<JiraBotReplyFormat>().joinToString { it.name.toLowerCase() }
    }
}
