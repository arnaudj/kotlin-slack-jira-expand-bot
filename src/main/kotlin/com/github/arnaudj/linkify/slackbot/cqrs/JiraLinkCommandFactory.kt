package com.github.arnaudj.linkify.slackbot.cqrs

import com.github.arnaudj.linkify.config.ConfigurationConstants
import com.github.arnaudj.linkify.cqrs.commands.Command
import com.github.arnaudj.linkify.cqrs.commands.CommandFactory
import com.github.arnaudj.linkify.slackbot.cqrs.commands.ResolveJiraCommand
import java.util.regex.Pattern

class JiraLinkCommandFactory(configMap: Map<String, Any>) : CommandFactory {
    val jiraHostBaseUrl = configMap[ConfigurationConstants.jiraHostBaseUrl] as String
    val resolveViaAPI = configMap[ConfigurationConstants.jiraResolveWithAPI] as Boolean
    val pattern: Pattern = Pattern.compile("([A-Za-z]{4,7}-\\d{1,5})+")

    override fun createFrom(message: String, channelId: String, userId: String): List<Command> {
        val command = extractJiraIssueReferences(message) ?: return emptyList()
        return listOf(ResolveJiraCommand(command, channelId, userId, jiraHostBaseUrl, resolveViaAPI))
    }

    private fun extractJiraIssueReferences(message: String): List<String>? {
        val ret = mutableListOf<String>()
        val matcher = pattern.matcher(message)

        while (matcher.find()) {
            val jiraRef = matcher.group(1)
            ret.add(jiraRef.toUpperCase())
        }

        return if (ret.isEmpty()) null else ret
    }
}
