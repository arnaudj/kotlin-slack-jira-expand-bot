package com.github.arnaudj.linkify.slackbot.cqrs.commands

import com.github.arnaudj.linkify.cqrs.commands.Command
import com.github.arnaudj.linkify.cqrs.events.Event
import com.github.arnaudj.linkify.slackbot.cqrs.events.JiraResolved
import com.github.arnaudj.linkify.slackbot.cqrs.results.JiraEntity


class ResolveJiraCommand(val jiraIdentifiers: List<String>, val channelId: String, val userId: String, val jiraHostBaseUrl: String,
                         val resolveViaAPI: Boolean) : Command {
    override fun execute(): List<Event> {
        println("ResolveJiraCommand.execute() - $jiraIdentifiers, $channelId")
        // TODO Link up with a iraResolutionService that will handle the resolution (via REST API) or link only
        // XXX use resolveViaAPI to resolve via JIRA Rest API
        println("resolveViaAPI = ${resolveViaAPI}")

        return listOf(JiraResolved(channelId, jiraIdentifiers.map { jiraId ->
            JiraEntity(jiraId, "<$jiraHostBaseUrl/$jiraId|$jiraId>")
        }))
    }
}
