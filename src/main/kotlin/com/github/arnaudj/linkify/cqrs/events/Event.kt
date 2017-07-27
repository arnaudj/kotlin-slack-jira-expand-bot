package com.github.arnaudj.linkify.cqrs.events

interface Event {
    val sourceId: String
}
