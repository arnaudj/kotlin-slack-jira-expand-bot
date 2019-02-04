package com.github.arnaudj.linkify.jiraengine

import com.github.arnaudj.eventdriven.commands.Command
import com.github.arnaudj.eventdriven.events.Event
import com.github.arnaudj.eventdriven.events.EventSourceData
import com.github.arnaudj.linkify.jiraengine.eventdriven.JiraEventFactory
import com.github.arnaudj.linkify.jiraengine.eventdriven.commands.ResolveJiraCommand
import com.github.arnaudj.linkify.jiraengine.eventdriven.events.JiraResolvedEvent
import com.github.arnaudj.linkify.jiraengine.eventdriven.events.JiraSeenEvent
import com.github.arnaudj.linkify.spi.jira.JiraEntity
import com.github.arnaudj.linkify.spi.jira.JiraKeyType
import com.github.arnaudj.linkify.spi.jira.JiraResolutionService
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import com.google.common.eventbus.DeadEvent
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.common.util.concurrent.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


interface AppEventHandler {
    fun onJiraResolvedEvent(event: JiraResolvedEvent, kodein: Kodein)
}

class JiraResolutionEngine(val kodein: Kodein, workerPoolSize: Int, val appEventHandler: AppEventHandler) {
    private val throttlingDelayMinutes = 2L
    val eventBus: EventBus = EventBus()
    val jiraService: JiraResolutionService = kodein.instance()
    val workerPool: ListeningExecutorService
    val jiraKeyToLastSeen: MutableMap<JiraKeyType, Long> = mutableMapOf()

    init {
        eventBus.register(this)
        workerPool = if (workerPoolSize > 0)
            MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(workerPoolSize))
        else
            MoreExecutors.listeningDecorator(MoreExecutors.newDirectExecutorService())
    }

    fun handleMessage(message: String, source: EventSourceData) =
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

    private fun shouldThrottle(event: JiraSeenEvent): Boolean {
        val now = System.currentTimeMillis()
        val key = event.entity.key
        val ret = (now - jiraKeyToLastSeen.getOrDefault(key, 0)) < TimeUnit.MINUTES.toMillis(throttlingDelayMinutes)
        jiraKeyToLastSeen[key] = now
        return ret
    }

    @Subscribe
    fun onEvent(event: Event) {
        when (event) {
            is JiraSeenEvent -> if (!shouldThrottle(event))
                postBusCommand(ResolveJiraCommand(event.entity.key, event.source, kodein))
            is JiraResolvedEvent -> appEventHandler.onJiraResolvedEvent(event, kodein)
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


}

