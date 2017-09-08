package com.github.arnaudj.linkify.slackbot.cqrs

import com.github.arnaudj.linkify.config.ConfigurationConstants
import com.github.arnaudj.linkify.cqrs.ReplyEventMapper
import com.github.arnaudj.linkify.slackbot.cqrs.events.JiraResolved
import com.github.arnaudj.linkify.spi.jira.JiraEntity

class JiraResolvedEventMapper : ReplyEventMapper<JiraResolved, String> {
    override fun map(event: JiraResolved, configMap: Map<String, Any>): String {
        val JiraHostURL = configMap[ConfigurationConstants.jiraHostBaseUrl]
        val entityMapper = { e: JiraEntity -> "<$JiraHostURL/${e.key}|${e.key}>" }
        return event.issues.joinToString(separator = "\n", limit = 30,
                transform = entityMapper)
    }
}
