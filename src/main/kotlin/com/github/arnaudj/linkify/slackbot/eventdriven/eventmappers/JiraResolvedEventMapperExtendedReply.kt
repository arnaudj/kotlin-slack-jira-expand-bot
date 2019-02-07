package com.github.arnaudj.linkify.slackbot.eventdriven.eventmappers

import com.github.arnaudj.linkify.engines.ReplyEventMapper
import com.github.arnaudj.linkify.engines.jira.entities.JiraEntity
import com.github.arnaudj.linkify.engines.jira.entities.JiraResolvedEvent
import com.ullink.slack.simpleslackapi.SlackAttachment
import com.ullink.slack.simpleslackapi.SlackPreparedMessage
import org.joda.time.format.DateTimeFormat

class JiraResolvedEventMapperExtendedReply :
        JiraResolvedEventMapperBase(), ReplyEventMapper<JiraResolvedEvent, List<SlackPreparedMessage>> {

    val priorityColorMap = mapOf("Minor" to "green", "Major" to "#439FE0", "Critical" to "warning", "Blocker" to "danger")

    override fun createEntityBuilder(jiraHostURL: String, event: JiraResolvedEvent): SlackPreparedMessage.Builder {
        val e = event.entity
        val attachment = SlackAttachment(
                "${e.key}: ${getTitle(e)}",
                getTitle(e),
                "", // text: Optional text that appears within the attachment
                "" // pretext: Optional text that appears above the attachment block
        )
        val priorityName = getFieldSafe(e, "priority.name")
        attachment.addField("Priority", priorityName, true)
        attachment.addField("Status", getFieldSafe(e, "status.name"), true)
        attachment.addField("Reporter", getFieldSafe(e, "reporter.name"), true)
        attachment.addField("Assignee", getFieldSafe(e, "assignee.name"), true)

        val updated = getFieldSafe(e, "updated")
        val footerPrefix = "Updated: "
        attachment.footer = if (updated.isNotEmpty()) formatDate(updated, footerPrefix) else footerPrefix

        attachment.color = priorityToColor(priorityName)
        attachment.titleLink = getIssueHref(jiraHostURL, e)

        return SlackPreparedMessage.Builder()
                //.withMessage() // common message to N attachments of this message
                .withAttachments(listOf(attachment))
    }

    fun getFieldSafe(entity: JiraEntity, name: String) = entity.fieldsMap[name] as? String ?: ""

    fun priorityToColor(priority: String) = priorityColorMap.getOrElse(priority, { "" })

    fun formatDate(date: String, messagePrefix: String): String {
        val epoch = DateTimeFormat
                .forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                .withOffsetParsed()
                .parseDateTime(date)
                .millis / 1000
        return "<!date^$epoch^${messagePrefix}{date_num} {time_secs}|$date>"
    }
}
