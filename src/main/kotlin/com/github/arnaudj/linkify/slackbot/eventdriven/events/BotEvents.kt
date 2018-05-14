package com.github.arnaudj.linkify.slackbot.eventdriven.events

import com.github.arnaudj.linkify.eventdriven.events.Event
import com.github.arnaudj.linkify.eventdriven.events.EventSourceData
import com.github.arnaudj.linkify.spi.jira.JiraEntity

data class JiraSeenEvent(override val source: EventSourceData, val entity: JiraEntity) : Event

data class JiraResolvedEvent(override val source: EventSourceData, val entity: JiraEntity) : Event
