package com.github.arnaudj.linkify.slackbot.eventdriven.eventmappers

import com.github.arnaudj.linkify.engines.ReplyEventMapper
import com.github.arnaudj.linkify.engines.jira.entities.JiraEntity
import com.github.arnaudj.linkify.engines.jira.entities.JiraResolvedEvent
import com.ullink.slack.simpleslackapi.SlackPreparedMessage

class JiraResolvedEventMapperShortReply : JiraResolvedEventMapperBase(), ReplyEventMapper<JiraResolvedEvent, List<SlackPreparedMessage>> {
    override fun createEntityBuilder(jiraHostURL: String, e: JiraResolvedEvent): SlackPreparedMessage.Builder {
        return SlackPreparedMessage.Builder()
                .withMessage(formatJiraIssueLinkAndSummary(jiraHostURL, e.entity))
    }

    private fun formatJiraIssueLinkAndSummary(jiraHostURL: String, e: JiraEntity) =
            formatLink(jiraHostURL, e) + backtickWrapper(getTitle(e, ""))

    private fun formatLink(jiraHostURL: String, e: JiraEntity) = "<${getIssueHref(jiraHostURL, e)}|${e.key}>"

    private fun backtickWrapper(s: String) = if (s.isNotEmpty()) " `$s`" else ""
}
