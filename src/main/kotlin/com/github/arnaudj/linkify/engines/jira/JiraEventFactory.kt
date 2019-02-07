package com.github.arnaudj.linkify.engines.jira

import com.github.arnaudj.linkify.eventdriven.commands.InboundMessageEventFactory
import com.github.arnaudj.linkify.eventdriven.events.Event
import com.github.arnaudj.linkify.eventdriven.events.EventSourceData
import com.github.arnaudj.linkify.engines.jira.entities.JiraEntity
import com.github.arnaudj.linkify.engines.jira.entities.JiraSeenEvent
import java.util.regex.Pattern

class JiraEventFactory : InboundMessageEventFactory {
    val pattern: Pattern = Pattern.compile("[A-Z][A-Z0-9]{1,9}-\\d+")

    override fun createFrom(message: String, source: EventSourceData): List<Event> {
        return extractJiraIssueReferences(message).map { key ->
            JiraSeenEvent(source, JiraEntity(key))
        }
    }

    private fun extractJiraIssueReferences(message: String): List<String> {
        val ret = mutableListOf<String>()
        val matcher = pattern.matcher(message)

        while (matcher.find()) {
            val jiraRef = matcher.group(1)
            ret.add(jiraRef.toUpperCase())
        }

        return if (ret.isEmpty()) emptyList() else ret
    }
}
