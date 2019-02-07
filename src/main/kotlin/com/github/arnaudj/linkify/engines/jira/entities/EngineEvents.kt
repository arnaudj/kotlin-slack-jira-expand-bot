package com.github.arnaudj.linkify.engines.jira.entities

import com.github.arnaudj.linkify.eventdriven.events.Event
import com.github.arnaudj.linkify.eventdriven.events.EventSourceData

data class JiraSeenEvent(override val source: EventSourceData, val entity: JiraEntity) : Event

data class JiraResolvedEvent(override val source: EventSourceData, val entity: JiraEntity) : Event
