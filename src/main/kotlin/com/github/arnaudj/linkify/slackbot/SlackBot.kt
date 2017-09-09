package com.github.arnaudj.linkify.slackbot

import com.github.arnaudj.linkify.config.ConfigurationConstants
import com.github.salomonbrys.kodein.Kodein
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import java.net.Proxy
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

// TODO Use Jira API to fetch issues information (JiraResolutionServiceImpl)
// TODO Use injector to fetch configuration (all)
// TODO Handle external configuration (jira host, token, watched jira project keys)

fun main(args: Array<String>) {

    val options = Options()
    options.addOption("t", true, "set bot auth token")
    options.addOption("p", true, "http proxy with format host:port")
    options.addOption("j", true, "jira http address")
    options.addOption("u", true, "use jira rest api to resolve issues information")
    options.addOption("h", false, "help")

    val cmdLine = DefaultParser().parse(options, args)
    val token = cmdLine.getOptionValue("t")
    if (cmdLine.hasOption("h") || (token?.length ?: -1) < 5) {
        HelpFormatter().printHelp("bot", options)
        return
    }

    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace")
    val proxy = cmdLine.getOptionValue("p")
    var jiraURL = cmdLine.getOptionValue("j")
    require(jiraURL?.toLowerCase()?.startsWith("http") ?: false, { "Missing/malformed Jira url. Exemple: http://localhost/jira/" })
    if (jiraURL.endsWith("/"))
        jiraURL = jiraURL.substring(0, jiraURL.length - 1)


    val commandsExecutorService = Executors.newSingleThreadScheduledExecutor()
    val eventsExecutorService = Executors.newSingleThreadScheduledExecutor()
    val configMap = mapOf(
            ConfigurationConstants.jiraHostBaseUrl to jiraURL,
            ConfigurationConstants.jiraResolveWithAPI to cmdLine.hasOption("u")
    )
    runBot(token, proxy, configMap, commandsExecutorService, eventsExecutorService)
}

private fun runBot(token: String?, proxy: String?, configMap: Map<String, Any>,
                   commandsExecutorService: ScheduledExecutorService,
                   eventsExecutorService: ScheduledExecutorService) {
    val session = SlackSessionFactory.getSlackSessionBuilder(token).apply {
        withAutoreconnectOnDisconnection(true)

        proxy?.let {
            println("* Using proxy: $it")
            val elmt = it.split(":", limit = 2)
            require(elmt.size == 2, { "malformed proxy" })
            withProxy(Proxy.Type.HTTP, elmt[0], elmt[1].toInt())
        }
    }.build()

    session.connect()
    println("* Bot connected")

    val kodein = Kodein {
        import(SlackbotModule.getInjectionBindings(configMap))
    }
    val bot = BotFacade(commandsExecutorService, kodein)

    eventsExecutorService.scheduleWithFixedDelay(
            {
                bot.handleEvents { eventReady ->
                    val markup = BotFacade.eventToMarkup(eventReady, configMap)
                    val channel = session.findChannelById(eventReady.sourceId)
                    session.sendMessage(channel, markup)
                }
            },
            0, 50, TimeUnit.MILLISECONDS
    )

    session.addMessagePostedListener(SlackMessagePostedListener { event, _ ->
        //if (event.channelId.id != session.findChannelByName("thechannel").id) return // target per channelId
        //if (event.sender.id != session.findUserByUserName("gueststar").id) return // target per user
        if (session.sessionPersona().id == event.sender.id)
            return@SlackMessagePostedListener // filter own messages, especially not to match own replies indefinitely

        with(event) {
            bot.handleMessage(messageContent, channel.id, user.id)
        }
    })
}

