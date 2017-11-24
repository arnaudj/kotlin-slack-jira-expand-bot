package com.github.arnaudj.linkify.eventdriven.commands

import com.github.arnaudj.linkify.eventdriven.events.Event

interface InboundMessageEventFactory {
    fun createFrom(message: String, channelId: String, userId: String): List<Event>
}
