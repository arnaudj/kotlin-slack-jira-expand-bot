package com.github.arnaudj.linkify.slackbot.eventdriven.mappers

import com.github.arnaudj.linkify.config.ConfigurationConstants
import com.github.arnaudj.linkify.slackbot.dtos.replies.JiraBotReplyMode
import com.github.arnaudj.linkify.slackbot.eventdriven.events.JiraResolvedEvent
import com.github.arnaudj.linkify.spi.jira.JiraEntity
import com.ullink.slack.simpleslackapi.SlackPreparedMessage

abstract class JiraResolvedEventMapperBase {
    abstract fun createEntityBuilder(jiraHostURL: String, e: JiraResolvedEvent): SlackPreparedMessage.Builder

    fun map(event: JiraResolvedEvent, configMap: Map<String, Any>): List<SlackPreparedMessage> {
        val jiraHostURL = configMap[ConfigurationConstants.jiraBrowseIssueBaseUrl] as String
        val jiraReferenceBotReplyMode = configMap[ConfigurationConstants.jiraReferenceBotReplyMode] as JiraBotReplyMode?

        val threadId = if (!event.source.threadId.isNullOrEmpty())
            event.source.threadId // original message already within a thread
        else when (jiraReferenceBotReplyMode) {
            JiraBotReplyMode.THREAD -> event.source.timestamp
            JiraBotReplyMode.INLINE -> null
            else -> throw IllegalArgumentException("Unsupported ReferenceReplyMode")
        }
        return listOf(createEntityBuilder(jiraHostURL, event)
                .withThreadTimestamp(threadId)
                .build())
    }

    fun getTitle(e: JiraEntity, defaultValue: String = "No summary found"): String =
            if (e.summary.isNotEmpty())
                e.summary
            else
                e.fieldsMap["summary"] as? String ?: defaultValue

    protected fun getIssueHref(jiraHostURL: String, e: JiraEntity) = "$jiraHostURL/${e.key}"
}
