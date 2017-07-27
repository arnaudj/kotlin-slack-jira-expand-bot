package com.github.arnaudj.linkify.cqrs.commands

class CommandDispatcher(vararg val cmdCreators: CommandFactory) : CommandFactory {
    override fun createFrom(message: String, channelId: String, userId: String): List<Command> =
            cmdCreators.flatMap {
                it.createFrom(message, channelId, userId)
            }
}
