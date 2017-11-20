package com.github.arnaudj.linkify.slackbot

import com.github.arnaudj.linkify.eventdriven.commands.Command
import com.github.arnaudj.linkify.eventdriven.commands.CommandDispatcher
import com.github.arnaudj.linkify.eventdriven.events.Event
import com.github.arnaudj.linkify.slackbot.eventdriven.ResolveJiraCommandFactory
import com.github.arnaudj.linkify.slackbot.eventdriven.commands.ResolveJiraCommand
import com.github.arnaudj.linkify.slackbot.eventdriven.events.JiraResolvedEvent
import com.github.arnaudj.linkify.slackbot.eventdriven.mappers.JiraBotReplyFormat
import com.github.arnaudj.linkify.slackbot.eventdriven.mappers.JiraResolvedEventMapperExtendedReply
import com.github.arnaudj.linkify.slackbot.eventdriven.mappers.JiraResolvedEventMapperShortReply
import com.github.arnaudj.linkify.spi.jira.JiraResolutionService
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import com.google.common.eventbus.DeadEvent
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.ullink.slack.simpleslackapi.SlackPreparedMessage


class BotFacade(kodein: Kodein, val appEventHandler: (Event) -> Unit) {

    val dispatcher = CommandDispatcher(ResolveJiraCommandFactory(kodein))
    var eventBus: EventBus
    val jiraService: JiraResolutionService = kodein.instance()

    init {
        eventBus = EventBus() // TODO allow to use AsyncEventBus for prod
        eventBus.register(this)
    }

    // FIXME have an extra step, so bot receives a JiraSeenEvent and decides to act on it/disregard
    fun handleChatMessage(message: String, sourceId: String, userId: String) = dispatcher.createFrom(message, sourceId, userId).forEach { postBusCommand(it) }

    @Subscribe
    fun onResolveJiraCommand(event: ResolveJiraCommand) {
        println("onResolveJiraCommand(): ${event}")
        postBusEvent(JiraResolvedEvent(event.sourceId, jiraService.resolve(event.key)))
    }

    @Subscribe
    fun onDeadEvent(event: Event) {
        appEventHandler(event)
    }

    @Subscribe
    fun onDeadEvent(event: DeadEvent) {
        error("Unexpected dead event: $event")
    }

    fun postBusCommand(command: Command) {
        println("postBusCommand = ${command}")
        eventBus.post(command)
    }

    fun postBusEvent(event: Event) {
        println("postBusEvent = ${event}")
        eventBus.post(event)
    }

    companion object BotFacade {
        fun createSlackMessageFromEvent(event: Event, configMap: Map<String, Any>, jiraBotReplyFormat: JiraBotReplyFormat): List<SlackPreparedMessage> {
            return when (event) {
                is JiraResolvedEvent ->
                    when (jiraBotReplyFormat) {
                        JiraBotReplyFormat.SHORT -> JiraResolvedEventMapperShortReply().map(event, configMap)
                        JiraBotReplyFormat.EXTENDED -> JiraResolvedEventMapperExtendedReply().map(event, configMap)
                    }
                else -> error("Unsupported event type")
            }
        }
    }
}
