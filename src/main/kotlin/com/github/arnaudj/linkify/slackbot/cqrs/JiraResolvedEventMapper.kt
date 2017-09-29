package com.github.arnaudj.linkify.slackbot.cqrs

import com.github.arnaudj.linkify.config.ConfigurationConstants
import com.github.arnaudj.linkify.cqrs.ReplyEventMapper
import com.github.arnaudj.linkify.slackbot.cqrs.events.JiraResolved
import com.github.arnaudj.linkify.spi.jira.JiraEntity

class JiraResolvedEventMapper : ReplyEventMapper<JiraResolved, String> {
    override fun map(event: JiraResolved, configMap: Map<String, Any>): String {
        val jiraHostURL = configMap[ConfigurationConstants.jiraBrowseIssueBaseUrl] as String
        return event.issues.joinToString(separator = "\n", limit = 30,
                transform = { entityMapper(jiraHostURL, it) })
    }

    fun entityMapper(jiraHostURL: String, e: JiraEntity): CharSequence {
        var link = "<$jiraHostURL/${e.key}|${e.key}>"
        return link + formatTitle(e)
    }

    private fun formatTitle(e: JiraEntity): String? {
        val backtickWrapper = { s: String -> if (s.isNotEmpty()) " `$s`" else "" }
        val rootTitle = backtickWrapper(e.summary)
        val nestedTitle: String? = backtickWrapper((e.fieldsMap["summary"] as? String) ?: "")
        return if (rootTitle.isNotEmpty()) rootTitle else nestedTitle
    }
}
