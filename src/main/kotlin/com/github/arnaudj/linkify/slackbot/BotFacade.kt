package com.github.arnaudj.linkify.slackbot

import com.github.arnaudj.linkify.eventdriven.commands.Command
import com.github.arnaudj.linkify.eventdriven.commands.CommandDispatcher
import com.github.arnaudj.linkify.eventdriven.events.Event
import com.github.arnaudj.linkify.slackbot.eventdriven.MessageToCommandFactory
import com.github.arnaudj.linkify.slackbot.eventdriven.commands.NotifyJiraSeenCommand
import com.github.arnaudj.linkify.slackbot.eventdriven.commands.ResolveJiraCommand
import com.github.arnaudj.linkify.slackbot.eventdriven.events.JiraResolvedEvent
import com.github.arnaudj.linkify.slackbot.eventdriven.events.JiraSeenEvent
import com.github.arnaudj.linkify.slackbot.eventdriven.mappers.JiraBotReplyFormat
import com.github.arnaudj.linkify.slackbot.eventdriven.mappers.JiraResolvedEventMapperExtendedReply
import com.github.arnaudj.linkify.slackbot.eventdriven.mappers.JiraResolvedEventMapperShortReply
import com.github.arnaudj.linkify.spi.jira.JiraEntity
import com.github.arnaudj.linkify.spi.jira.JiraResolutionService
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import com.google.common.eventbus.DeadEvent
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.ullink.slack.simpleslackapi.SlackPreparedMessage


interface AppEventHandler {
    fun onJiraSeenEvent(event: JiraSeenEvent, bot: BotFacade, kodein: Kodein)
    fun onJiraResolvedEvent(event: JiraResolvedEvent, bot: BotFacade, kodein: Kodein)

    fun doDefaultOnJiraSeenEvent(event: JiraSeenEvent, bot: BotFacade, kodein: Kodein) {
        bot.postBusCommand(ResolveJiraCommand(event.entity.key, event.sourceId, kodein))
    }
}

class BotFacade(val kodein: Kodein, val appEventHandler: AppEventHandler) {

    val dispatcher = CommandDispatcher(MessageToCommandFactory(kodein))
    var eventBus: EventBus
    val jiraService: JiraResolutionService = kodein.instance()

    init {
        eventBus = EventBus() // TODO allow to use AsyncEventBus for prod
        eventBus.register(this)
    }

    fun handleChatMessage(message: String, sourceId: String, userId: String) =
            dispatcher.createFrom(message, sourceId, userId).forEach { postBusCommand(it) }

    @Subscribe
    fun onNotifyJiraSeenCommand(event: NotifyJiraSeenCommand) {
        println("onNotifyJiraSeenCommand(): ${event}")
        postBusEvent(JiraSeenEvent(event.sourceId, JiraEntity(event.key, "")))
    }

    @Subscribe
    fun onResolveJiraCommand(event: ResolveJiraCommand) {
        println("onResolveJiraCommand(): ${event}")

        // TODO Dethread here, to avoid event bus congestion, as we use jiraService from an eventbus dispatcher thread here (regardless of sync or async)
        postBusEvent(JiraResolvedEvent(event.sourceId, jiraService.resolve(event.key)))
    }

    @Subscribe
    fun onEvent(event: Event) {
        when (event) {
            is JiraSeenEvent -> appEventHandler.onJiraSeenEvent(event, this, kodein)
            is JiraResolvedEvent -> appEventHandler.onJiraResolvedEvent(event, this, kodein)
            else -> error("Unsupported event in bot: $event")
        }
    }

    @Subscribe
    fun onDeadEvent(event: DeadEvent) {
        error("Unexpected dead event: $event")
    }

    fun postBusCommand(command: Command) {
        println("> postBusCommand: ${command}")
        eventBus.post(command)
    }

    fun postBusEvent(event: Event) {
        println("> postBusEvent: ${event}")
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
