package com.github.arnaudj.linkify.slackbot.cqrs.commands

import com.github.arnaudj.linkify.cqrs.commands.Command
import com.github.arnaudj.linkify.cqrs.events.Event
import com.github.arnaudj.linkify.slackbot.cqrs.events.JiraResolved
import com.github.arnaudj.linkify.spi.jira.JiraResolutionService
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance


class ResolveJiraCommand(val jiraIdentifiers: List<String>, val channelId: String, val userId: String, kodein: Kodein) : Command {
    val jiraService: JiraResolutionService = kodein.instance()

    override fun execute(): List<Event> {
        return listOf(JiraResolved(channelId, jiraIdentifiers.map { jiraId ->
            jiraService.resolve(jiraId)
        }))
    }
}
