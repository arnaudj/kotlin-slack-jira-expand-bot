package com.github.arnaudj.linkify.slackbot

import com.github.arnaudj.linkify.cqrs.DataStore
import com.github.arnaudj.linkify.cqrs.commands.CommandDispatcher
import com.github.arnaudj.linkify.cqrs.events.Event
import com.github.arnaudj.linkify.slackbot.cqrs.JiraLinkCommandFactory
import com.github.arnaudj.linkify.slackbot.cqrs.events.JiraResolved
import com.github.arnaudj.linkify.slackbot.cqrs.mappers.JiraBotReplyFormat
import com.github.arnaudj.linkify.slackbot.cqrs.mappers.JiraResolvedEventMapperExtendedReply
import com.github.arnaudj.linkify.slackbot.cqrs.mappers.JiraResolvedEventMapperShortReply
import com.github.salomonbrys.kodein.Kodein
import com.ullink.slack.simpleslackapi.SlackPreparedMessage
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class BotFacade(val executor: Executor, kodein: Kodein) {

    val dispatcher = CommandDispatcher(JiraLinkCommandFactory(kodein))
    val eventStore = DataStore<Event>()

    fun handleMessage(message: String, channelId: String, userId: String) {
        dispatcher.createFrom(message, channelId, userId).forEach { command ->
            executor.execute {
                val events = command.execute()
                eventStore.store.addAll(events)
            }
        }
    }

    fun handleEvents(action: (Event) -> Unit) {
        tilEmpty@ while (true) {
            val event = eventStore.store.poll(50, TimeUnit.MILLISECONDS) ?: break@tilEmpty
            action.invoke(event)
        }
    }

    companion object BotFacade {
        fun createSlackMessageFromEvent(event: Event, configMap: Map<String, Any>, jiraBotReplyFormat: JiraBotReplyFormat): List<SlackPreparedMessage> {
            return when (event) {
                is JiraResolved ->
                    when (jiraBotReplyFormat) {
                        JiraBotReplyFormat.SHORT -> JiraResolvedEventMapperShortReply().map(event, configMap)
                        JiraBotReplyFormat.EXTENDED -> JiraResolvedEventMapperExtendedReply().map(event, configMap)
                    }
                else -> error("Unsupported event type")
            }
        }
    }
}
