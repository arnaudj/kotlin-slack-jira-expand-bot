package com.github.arnaudj.linkify.eventdriven.commands

import com.github.arnaudj.linkify.eventdriven.events.Event
import com.github.arnaudj.linkify.eventdriven.events.EventSourceData

interface InboundMessageEventFactory {
    fun createFrom(message: String, source: EventSourceData): List<Event>
}
