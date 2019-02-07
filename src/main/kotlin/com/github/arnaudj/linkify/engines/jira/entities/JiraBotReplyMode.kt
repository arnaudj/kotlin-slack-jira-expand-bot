package com.github.arnaudj.linkify.engines.jira.entities

enum class JiraBotReplyMode {
    INLINE,
    THREAD;

    companion object {
        fun knownValues(): String = enumValues<JiraBotReplyMode>().joinToString { it.name.toLowerCase() }
    }
}
