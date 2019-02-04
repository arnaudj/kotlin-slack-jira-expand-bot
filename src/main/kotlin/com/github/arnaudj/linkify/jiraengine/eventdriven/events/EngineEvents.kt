package com.github.arnaudj.linkify.jiraengine.eventdriven.events

import com.github.arnaudj.eventdriven.events.Event
import com.github.arnaudj.eventdriven.events.EventSourceData
import com.github.arnaudj.linkify.spi.jira.JiraEntity

data class JiraSeenEvent(override val source: EventSourceData, val entity: JiraEntity) : Event

data class JiraResolvedEvent(override val source: EventSourceData, val entity: JiraEntity) : Event
