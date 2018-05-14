package com.github.arnaudj.linkify.eventdriven.events

data class EventSourceData(val sourceId: String, val userId: String, val timestamp: String, val threadId: String?)

interface Event {
    val source: EventSourceData
}
