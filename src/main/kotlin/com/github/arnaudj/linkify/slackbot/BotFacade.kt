package com.github.arnaudj.linkify.slackbot

import com.github.arnaudj.linkify.cqrs.DataStore
import com.github.arnaudj.linkify.cqrs.commands.CommandDispatcher
import com.github.arnaudj.linkify.cqrs.events.Event
import com.github.arnaudj.linkify.cqrs.events.JiraResolved
import com.github.arnaudj.linkify.slackbot.cqrs.JiraLinkCommandFactory
import com.github.arnaudj.linkify.slackbot.cqrs.JiraResolvedEventMapper
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

class BotFacade(val executorService: ExecutorService, jiraHostBaseUrl: String) {
    val dispatcher = CommandDispatcher(JiraLinkCommandFactory(jiraHostBaseUrl))
    val eventStore = DataStore<Event>()

    fun handleMessage(message: String, channelId: String, userId: String) {
        val commands = dispatcher.createFrom(message, channelId, userId)
        commands.forEach { command ->
            executorService.execute {
                val events = command.execute()
                eventStore.store.addAll(events)
            }
        }
    }

    fun handleEvents(action: (Event) -> Unit) {
        var keep: Boolean
        do {
            keep = false
            eventStore.store.poll(50, TimeUnit.MILLISECONDS)?.let {
                action.invoke(it)
                keep = true
            }
        } while (keep)
    }

    companion object BotFacade {
        fun eventToMarkup(event: Event, configMap: Map<String, Any>): String {
            return when (event) {
                is JiraResolved -> JiraResolvedEventMapper().map(event, configMap)
                else -> "Unsupported event type"
            }
        }
    }
}
