package com.github.arnaudj.linkify.slackbot.eventdriven.eventmappers

import com.github.arnaudj.linkify.engines.ReplyEventMapper
import com.github.arnaudj.linkify.engines.jira.entities.JiraEntity
import com.github.arnaudj.linkify.engines.jira.entities.JiraResolvedEvent
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.model.Attachment
import com.slack.api.model.Field
import org.joda.time.format.DateTimeFormat

class JiraResolvedEventMapperExtendedReply :
        JiraResolvedEventMapperBase(), ReplyEventMapper<JiraResolvedEvent, List<ChatPostMessageRequest>> {

    val priorityColorMap = mapOf("Minor" to "green", "Major" to "#439FE0", "Critical" to "warning", "Blocker" to "danger")

    override fun createEntityBuilder(jiraHostURL: String, event: JiraResolvedEvent): ChatPostMessageRequest.ChatPostMessageRequestBuilder {
        val e = event.entity
        val attachment = Attachment()
        attachment.title = "${e.key}: ${getTitle(e)}"
        attachment.fallback = getTitle(e)

        val fieldDefinitions = listOf(
            "Priority" to "priority.name",
            "Status" to "status.name",
            "Reporter" to "reporter.name",
            "Assignee" to "assignee.name"
        )
        attachment.fields = fieldDefinitions.map { (title, path) ->
            Field().apply {
                this.title = title
                this.value = getFieldSafe(e, path)
                this.isValueShortEnough = true
            }
        }

        val updated = getFieldSafe(e, "updated")
        val footerPrefix = "Updated: "
        attachment.footer = if (updated.isNotEmpty()) formatDate(updated, footerPrefix) else footerPrefix

        attachment.color = priorityToColor(getFieldSafe(e, "priority.name"))
        attachment.titleLink = getIssueHref(jiraHostURL, e)

        return ChatPostMessageRequest.builder()
                //.withMessage() // common message to N attachments of this message
                .attachments(listOf(attachment))
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
