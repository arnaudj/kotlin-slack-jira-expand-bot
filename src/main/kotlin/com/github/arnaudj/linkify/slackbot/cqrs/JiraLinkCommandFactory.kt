package com.github.arnaudj.linkify.slackbot.cqrs

import com.github.arnaudj.linkify.cqrs.commands.Command
import com.github.arnaudj.linkify.cqrs.commands.CommandFactory
import com.github.arnaudj.linkify.slackbot.cqrs.commands.ResolveJiraCommand
import com.github.salomonbrys.kodein.Kodein
import java.util.regex.Pattern

class JiraLinkCommandFactory(val kodein: Kodein) : CommandFactory {
    val pattern: Pattern = Pattern.compile("([A-Za-z]{4,7}-\\d{1,5})+")

    override fun createFrom(message: String, channelId: String, userId: String): List<Command> {
        return extractJiraIssueReferences(message).map { key ->
            ResolveJiraCommand(key, channelId, userId, kodein)
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
