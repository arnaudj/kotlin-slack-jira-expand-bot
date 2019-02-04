package com.github.arnaudj.eventdriven.commands

import com.github.arnaudj.eventdriven.events.Event
import com.github.arnaudj.eventdriven.events.EventSourceData

interface InboundMessageEventFactory {
    fun createFrom(message: String, source: EventSourceData): List<Event>
}
