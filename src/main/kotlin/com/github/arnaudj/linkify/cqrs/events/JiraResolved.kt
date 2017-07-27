package com.github.arnaudj.linkify.cqrs.events

import com.github.arnaudj.linkify.cqrs.results.JiraEntity


class JiraResolved(override val sourceId: String, val issues: List<JiraEntity>) : Event
