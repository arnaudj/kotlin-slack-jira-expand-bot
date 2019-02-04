package com.github.arnaudj.linkify.jiraengine.dtos.replies

enum class JiraBotReplyMode {
    INLINE,
    THREAD;

    companion object {
        fun knownValues(): String = enumValues<JiraBotReplyMode>().joinToString { it.name.toLowerCase() }
    }
}
