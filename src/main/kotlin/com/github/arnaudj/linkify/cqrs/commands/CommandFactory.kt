package com.github.arnaudj.linkify.cqrs.commands

interface CommandFactory {
    fun createFrom(message: String, channelId: String, userId: String): List<Command>
}
