package com.github.arnaudj.linkify.cqrs.commands

import com.github.arnaudj.linkify.cqrs.events.Event

interface Command {
    fun execute(): List<Event>
}
