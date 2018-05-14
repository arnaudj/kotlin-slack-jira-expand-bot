package com.github.arnaudj.linkify.slackbot.eventdriven.mappers

import com.github.arnaudj.linkify.config.ConfigurationConstants
import com.github.arnaudj.linkify.slackbot.eventdriven.events.JiraResolvedEvent
import com.github.arnaudj.linkify.spi.jira.JiraEntity
import com.ullink.slack.simpleslackapi.SlackPreparedMessage

abstract class JiraResolvedEventMapperBase {
    abstract fun mapEntity(jiraHostURL: String, e: JiraEntity): SlackPreparedMessage

    fun map(event: JiraResolvedEvent, configMap: Map<String, Any>): List<SlackPreparedMessage> {
        val jiraHostURL = configMap[ConfigurationConstants.jiraBrowseIssueBaseUrl] as String
        return listOf(mapEntity(jiraHostURL, event.entity))
    }

    fun getTitle(e: JiraEntity, defaultValue: String = "No summary found"): String =
            if (e.summary.isNotEmpty())
                e.summary
            else
                e.fieldsMap["summary"] as? String ?: defaultValue

    protected fun getIssueHref(jiraHostURL: String, e: JiraEntity) = "$jiraHostURL/${e.key}"
}