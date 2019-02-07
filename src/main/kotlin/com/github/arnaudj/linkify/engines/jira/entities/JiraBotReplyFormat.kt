package com.github.arnaudj.linkify.engines.jira.entities

enum class JiraBotReplyFormat {
    SHORT,
    EXTENDED;

    companion object {
        fun knownValues(): String = enumValues<JiraBotReplyFormat>().joinToString { it.name.toLowerCase() }
    }
}
