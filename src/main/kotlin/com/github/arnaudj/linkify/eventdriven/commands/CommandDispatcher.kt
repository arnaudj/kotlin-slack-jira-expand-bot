package com.github.arnaudj.linkify.eventdriven.commands

class CommandDispatcher(vararg val cmdCreators: CommandFactory) : CommandFactory {
    override fun createFrom(message: String, sourceId: String, userId: String): List<Command> =
            cmdCreators.flatMap {
                it.createFrom(message, sourceId, userId)
            }
}
