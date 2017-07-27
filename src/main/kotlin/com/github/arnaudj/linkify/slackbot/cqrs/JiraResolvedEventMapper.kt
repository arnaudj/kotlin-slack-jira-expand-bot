package com.github.arnaudj.linkify.slackbot.cqrs

import com.github.arnaudj.linkify.cqrs.events.JiraResolved
import com.github.arnaudj.linkify.cqrs.results.JiraEntity

class JiraResolvedEventMapper : ReplyEventMapper<JiraResolved, String> {
    override fun map(event: JiraResolved): String { // FIXME propagage jiraHostURL
        val entityMapper = { e: JiraEntity -> "<FAKE/${e.key}|${e.key}>" }
        return event.issues.joinToString(separator = "\n", limit = 30,
                transform = entityMapper)
    }
}
