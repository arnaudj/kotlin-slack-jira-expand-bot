package com.github.arnaudj.linkify.slackbot.eventdriven.events

import com.github.arnaudj.linkify.eventdriven.events.Event
import com.github.arnaudj.linkify.spi.jira.JiraEntity

data class JiraSeenEvent(override val sourceId: String, val entity: JiraEntity) : Event

data class JiraResolvedEvent(override val sourceId: String, val entity: JiraEntity) : Event
