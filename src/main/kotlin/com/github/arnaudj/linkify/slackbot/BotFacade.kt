package com.github.arnaudj.linkify.slackbot

import com.github.arnaudj.linkify.eventdriven.commands.Command
import com.github.arnaudj.linkify.eventdriven.events.Event
import com.github.arnaudj.linkify.eventdriven.events.EventSourceData
import com.github.arnaudj.linkify.slackbot.dtos.replies.JiraBotReplyFormat
import com.github.arnaudj.linkify.slackbot.eventdriven.JiraEventFactory
import com.github.arnaudj.linkify.slackbot.eventdriven.commands.ResolveJiraCommand
import com.github.arnaudj.linkify.slackbot.eventdriven.events.JiraResolvedEvent
import com.github.arnaudj.linkify.slackbot.eventdriven.events.JiraSeenEvent
import com.github.arnaudj.linkify.slackbot.eventdriven.mappers.JiraResolvedEventMapperExtendedReply
import com.github.arnaudj.linkify.slackbot.eventdriven.mappers.JiraResolvedEventMapperShortReply
import com.github.arnaudj.linkify.spi.jira.JiraEntity
import com.github.arnaudj.linkify.spi.jira.JiraResolutionService
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import com.google.common.eventbus.DeadEvent
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.common.util.concurrent.*
import com.ullink.slack.simpleslackapi.SlackPreparedMessage
import java.util.concurrent.Callable
import java.util.concurrent.Executors


interface AppEventHandler {
    fun onJiraSeenEvent(event: JiraSeenEvent, bot: BotFacade, kodein: Kodein)
    fun onJiraResolvedEvent(event: JiraResolvedEvent, bot: BotFacade, kodein: Kodein)

    fun doDefaultOnJiraSeenEvent(event: JiraSeenEvent, bot: BotFacade, kodein: Kodein) {
        bot.postBusCommand(ResolveJiraCommand(event.entity.key, event.source, kodein))
    }
}

class BotFacade(val kodein: Kodein, workerPoolSize: Int, val appEventHandler: AppEventHandler) {
    val eventBus: EventBus = EventBus()
    val jiraService: JiraResolutionService = kodein.instance()
    val workerPool: ListeningExecutorService

    init {
        eventBus.register(this)
        workerPool = if (workerPoolSize > 0)
            MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(workerPoolSize))
        else
            MoreExecutors.listeningDecorator(MoreExecutors.newDirectExecutorService())
    }

    fun handleChatMessage(message: String, source: EventSourceData) =
            arrayOf(JiraEventFactory())
                    .flatMap { factory -> factory.createFrom(message, source) }
                    .forEach { event -> postBusEvent(event) }

    @Subscribe
    fun onResolveJiraCommand(event: ResolveJiraCommand) {
        println("onResolveJiraCommand() handling ${event}")
        val future: ListenableFuture<JiraEntity>? = workerPool.submit(Callable<JiraEntity> { jiraService.resolve(event.key) })

        Futures.addCallback(future, object : FutureCallback<JiraEntity> {
            override fun onSuccess(result: JiraEntity?) {
                postBusEvent(JiraResolvedEvent(event.source, result!!))
            }

            override fun onFailure(t: Throwable?) {
                System.err.println("Unable to resolve for event: $event")
                t?.printStackTrace()
            }
        }, workerPool)
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
        @JvmStatic
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

        @JvmStatic
        fun createConfigMap(config: Map<String, Any>): Map<String, Any> = config
    }
}
