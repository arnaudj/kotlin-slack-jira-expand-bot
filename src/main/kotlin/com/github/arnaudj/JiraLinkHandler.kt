package com.github.arnaudj

import com.github.arnaudj.dtos.results.ReplyResult
import com.github.arnaudj.dtos.results.noopReply
import com.github.arnaudj.dtos.results.sendMessageReply
import java.util.regex.Pattern

class JiraLinkHandler(val jiraHostBaseUrl: String) : CommandHandler {
    val pattern: Pattern = Pattern.compile("([A-Za-z]{4,7}-\\d{1,5})+")

    override fun handleMessage(message: String, channelId: String, userId: String): List<ReplyResult> {
        val references = extractIssueReferences(message)

        if (references.isEmpty())
            return listOf(noopReply)

        return references.map { reference ->
            sendMessageReply.copy(channelId = channelId, message = "<$jiraHostBaseUrl/$reference|$reference>")
        }
    }

    fun extractIssueReferences(message: String): List<String> {
        val ret = mutableListOf<String>()
        val matcher = pattern.matcher(message)

        while (matcher.find()) {
            val jiraRef = matcher.group(1)
            ret.add(jiraRef.toUpperCase())
        }

        return ret
    }
}