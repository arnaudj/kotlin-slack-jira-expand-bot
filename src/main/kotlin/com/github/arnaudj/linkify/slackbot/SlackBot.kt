package com.github.arnaudj.linkify.slackbot

import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import java.net.Proxy

// TODO Handle external configuration (jira host, token, watched jira project keys)
// TODO Use Jira API to fetch issues information
// TODO Extract options handling

fun main(args: Array<String>) {

    val options = Options()
    options.addOption("t", true, "set bot auth token")
    options.addOption("p", true, "http proxy with format host:port")
    options.addOption("j", true, "jira http address")
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

    runBot(token, proxy, jiraURL)
}

private fun runBot(token: String?, proxy: String?, jiraHostBaseUrl: String) {
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

    val bot = BotFacade(jiraHostBaseUrl) { eventReady ->
        val markup = BotFacade.eventToMarkup(eventReady)
        val channel = session.findChannelById(eventReady.sourceId)
        session.sendMessage(channel, markup)
    }
    bot.start()

    session.addMessagePostedListener(SlackMessagePostedListener { event, session ->
        //if (event.channelId.id != session.findChannelByName("thechannel").id) return // target per channelId
        //if (event.sender.id != session.findUserByUserName("gueststar").id) return // target per user
        if (session.sessionPersona().id == event.sender.id)
            return@SlackMessagePostedListener // filter own messages, especially not to match own replies indefinitely

        with(event) {
            bot.handleMessage(messageContent, channel.id, user.id)
        }
    })
}

