package com.github.arnaudj.linkify.slackbot.cqrs.events

import com.github.arnaudj.linkify.cqrs.events.Event
import com.github.arnaudj.linkify.slackbot.cqrs.results.JiraEntity

class JiraResolved(override val sourceId: String, val issues: List<JiraEntity>) : Event
