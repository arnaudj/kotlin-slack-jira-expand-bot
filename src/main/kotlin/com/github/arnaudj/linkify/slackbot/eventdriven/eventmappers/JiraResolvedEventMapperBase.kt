package com.github.arnaudj.linkify.slackbot.eventdriven.eventmappers

import com.github.arnaudj.linkify.engines.jira.ConfigurationConstants
import com.github.arnaudj.linkify.engines.jira.entities.JiraEntity
import com.github.arnaudj.linkify.engines.jira.entities.JiraBotReplyMode
import com.github.arnaudj.linkify.engines.jira.entities.JiraResolvedEvent
import com.slack.api.methods.request.chat.ChatPostMessageRequest

abstract class JiraResolvedEventMapperBase {
    abstract fun createEntityBuilder(jiraHostURL: String, e: JiraResolvedEvent): ChatPostMessageRequest.ChatPostMessageRequestBuilder

    fun map(event: JiraResolvedEvent, configMap: Map<String, Any>): List<ChatPostMessageRequest> {
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
                .threadTs(threadId)
                .build())
    }

    fun getTitle(e: JiraEntity, defaultValue: String = "No summary found"): String =
        e.summary.ifEmpty { e.fieldsMap["summary"] as? String ?: defaultValue }

    protected fun getIssueHref(jiraHostURL: String, e: JiraEntity) = "$jiraHostURL/${e.key}"
}
