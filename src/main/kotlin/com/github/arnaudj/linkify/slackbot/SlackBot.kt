package com.github.arnaudj.linkify.slackbot

import com.github.arnaudj.linkify.config.ConfigurationConstants.jiraBrowseIssueBaseUrl
import com.github.arnaudj.linkify.config.ConfigurationConstants.jiraRestServiceAuthPassword
import com.github.arnaudj.linkify.config.ConfigurationConstants.jiraRestServiceAuthUser
import com.github.arnaudj.linkify.config.ConfigurationConstants.jiraRestServiceBaseUrl
import com.github.salomonbrys.kodein.Kodein
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import java.net.Proxy
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

// TODO JiraResolvedEventMapper: Include more information (criticity, author name & avatar (fields.reporter.avatarUrls))
// TODO Use injector to fetch configuration per token
// TODO Handle external configuration (jira host, token, watched jira project keys)

fun main(args: Array<String>) {

    val options = Options()
    options.addOption("t", true, "set bot auth token")
    options.addOption("p", true, "http proxy with format host:port")
    options.addOption("jia", true, "jira http base address for issues browsing (ex: http://jira.nodomain/browse)")
    options.addOption("jrs", true, "jira http base address for rest service (ex: http://jira.nodomain, without '/rest/api/latest/')")
    options.addOption("u", true, "jira credentials to resolve issues information, with format user:password")
    options.addOption("h", false, "help")

    val cmdLine = DefaultParser().parse(options, args)
    val token = cmdLine.getOptionValue("t")
    if (cmdLine.hasOption("h") || (token?.length ?: -1) < 5) {
        HelpFormatter().printHelp("bot", options)
        return
    }

    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace")
    val proxy = cmdLine.getOptionValue("p")

    val commandsExecutorService = Executors.newSingleThreadScheduledExecutor()
    val eventsExecutorService = Executors.newSingleThreadScheduledExecutor()
    val (jiraUser, jiraPassword) = extractJiraCredentials(cmdLine)
    val configMap = mapOf(
            jiraBrowseIssueBaseUrl to validateOptionUrl(cmdLine, "jia"),
            jiraRestServiceBaseUrl to validateOptionUrl(cmdLine, "jrs"),
            jiraRestServiceAuthUser to jiraUser,
            jiraRestServiceAuthPassword to jiraPassword
    )
    runBot(token, proxy, configMap, commandsExecutorService, eventsExecutorService)
}

private fun extractJiraCredentials(cmdLine: CommandLine): List<String> {
    cmdLine.getOptionValue("u")?.let {
        it.split(delimiters = ":", limit = 2).let {
            if (it.size == 2)
                return it
        }
    }
    return listOf("", "")
}

private fun validateOptionUrl(cmdLine: CommandLine, option: String): String {
    requireOption(cmdLine, option)
    return validateUrl(cmdLine.getOptionValue(option))
}

private fun validateUrl(url: String?): String {
    require(url?.toLowerCase()?.startsWith("http") ?: false, { "Missing/malformed url. Exemple: http://localhost/" })
    //require(url?.toLowerCase()?.startsWith("http"), { "Missing/malformed url. Exemple: http://localhost/" })
    return if (url!!.endsWith("/"))
        url.substring(0, url.length - 1)
    else
        url
}

private fun requireOption(cmdLine: CommandLine, option: String) {
    require(cmdLine.hasOption(option), { "Missing mandatory option: $option" })
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

    println("* Using bot configuration: ${configMap.toList().filter { it.first != jiraRestServiceAuthPassword }.toMap()}")

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

