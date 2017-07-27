package com.github.arnaudj.linkify.slackbot

import com.github.arnaudj.linkify.cqrs.DataStore
import com.github.arnaudj.linkify.cqrs.commands.Command
import com.github.arnaudj.linkify.cqrs.commands.CommandDispatcher
import com.github.arnaudj.linkify.cqrs.events.Event
import com.github.arnaudj.linkify.cqrs.events.JiraResolved
import com.github.arnaudj.linkify.slackbot.cqrs.JiraLinkCommandFactory
import com.github.arnaudj.linkify.slackbot.cqrs.JiraResolvedEventMapper
import java.util.concurrent.TimeUnit

class BotFacade(jiraHostBaseUrl: String, val eventSink: (Event) -> Unit) {
    val commandsStore = DataStore("commands", this::handleCommands)
    val eventStore = DataStore("events", this::handleEvents)
    val dispatcher = CommandDispatcher(JiraLinkCommandFactory(jiraHostBaseUrl))

    fun start() {
        eventStore.start()
        commandsStore.start()
    }

    fun stop() {
        eventStore.stop()
        commandsStore.stop()
    }

    fun handleMessage(message: String, channelId: String, userId: String) {
        val commands = dispatcher.createFrom(message, channelId, userId)
        commandsStore.store.addAll(commands)
    }

    private fun handleCommands(cmdStore: DataStore<Command>) {
        try {
            while (!Thread.currentThread().isInterrupted) {
                cmdStore.store.poll(1, TimeUnit.SECONDS)?.let { event ->
                    val events = event.execute()
                    println("handleCommands() executed, got events: - $events")
                    eventStore.store.addAll(events)
                }
            }
        } catch (noop: InterruptedException) {
        }
    }

    private fun handleEvents(evtStore: DataStore<Event>) {
        try {
            while (!Thread.currentThread().isInterrupted) {
                evtStore.store.poll(1, TimeUnit.SECONDS)?.let {
                    eventSink.invoke(it)
                }
            }
        } catch (noop: InterruptedException) {
        }
    }

    companion object BotFacade {
        fun eventToMarkup(event: Event): String {
            val markup = when (event) {
                is JiraResolved -> JiraResolvedEventMapper().map(event)
                else -> "Unsupported event type"
            }
            return markup
        }
    }
}
