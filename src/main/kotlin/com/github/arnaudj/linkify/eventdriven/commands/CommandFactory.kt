package com.github.arnaudj.linkify.eventdriven.commands

interface CommandFactory {
    fun createFrom(message: String, channelId: String, userId: String): List<Command>
}
