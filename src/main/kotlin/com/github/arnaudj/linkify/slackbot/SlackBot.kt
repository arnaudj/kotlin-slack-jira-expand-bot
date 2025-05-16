package com.github.arnaudj.linkify.slackbot

import com.github.arnaudj.linkify.engines.jira.AppEventHandler
import com.github.arnaudj.linkify.engines.jira.ConfigurationConstants.clientProxyHost
import com.github.arnaudj.linkify.engines.jira.ConfigurationConstants.clientProxyPort
import com.github.arnaudj.linkify.engines.jira.ConfigurationConstants.jiraBrowseIssueBaseUrl
import com.github.arnaudj.linkify.engines.jira.ConfigurationConstants.jiraReferenceBotReplyMode
import com.github.arnaudj.linkify.engines.jira.ConfigurationConstants.jiraRestServiceAuthPassword
import com.github.arnaudj.linkify.engines.jira.ConfigurationConstants.jiraRestServiceAuthUser
import com.github.arnaudj.linkify.engines.jira.ConfigurationConstants.jiraRestServiceBaseUrl
import com.github.arnaudj.linkify.engines.jira.entities.JiraBotReplyFormat
import com.github.arnaudj.linkify.engines.jira.entities.JiraBotReplyMode
import com.github.arnaudj.linkify.engines.jira.entities.JiraResolvedEvent
import com.github.arnaudj.linkify.slackbot.BotFacade.Companion.createSlackMessageFromEvent
import com.github.arnaudj.linkify.slackbot.SlackbotModule.Companion.getInjectionBindings
import com.github.arnaudj.linkify.slackbot.listener.MessageChangedListener
import com.github.arnaudj.linkify.slackbot.listener.MessageListener
import com.github.salomonbrys.kodein.Kodein
import com.slack.api.bolt.App
import com.slack.api.bolt.AppConfig
import com.slack.api.bolt.socket_mode.SocketModeApp
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.methods.response.chat.ChatPostMessageResponse
import com.slack.api.model.event.MessageChangedEvent
import com.slack.api.model.event.MessageEvent
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger("SlackBot")

fun main(args: Array<String>) {

    val options = Options()
    options.addOption("t", true, "set bot auth token")
    options.addOption("a", true, "set app auth token")
    options.addOption("p", true, "http proxy with format host:port")
    options.addOption("jia", true, "jira http base address for issues browsing (ex: http://jira.nodomain/browse)")
    options.addOption("jrs", true, "jira http base address for rest service (ex: http://jira.nodomain, without '/rest/api/latest/')")
    options.addOption("jfmt", true, "jira bot replies format: ${JiraBotReplyFormat.knownValues()}")
    options.addOption("jmode", true, "jira bot replies mode: ${JiraBotReplyMode.knownValues()}")
    options.addOption("u", true, "jira credentials to resolve issues information, with format user:password")
    options.addOption("h", false, "help")

    val cmdLine = DefaultParser().parse(options, args)
    val botToken = cmdLine.getOptionValue("t")
    val appToken = cmdLine.getOptionValue("a")
    if (cmdLine.hasOption("h") || (botToken?.length ?: -1) < 5) {
        HelpFormatter().printHelp("bot", options)
        return
    }

    val proxy = extractProxy(cmdLine)
    val (jiraUser, jiraPassword) = extractJiraCredentials(cmdLine)
    val jiraBotRepliesFormat = extractEnumOption(cmdLine, "jfmt", { JiraBotReplyFormat.valueOf(it) }) as JiraBotReplyFormat
    val jiraBotRepliesMode = extractEnumOption(cmdLine, "jmode", { JiraBotReplyMode.valueOf(it) }) as JiraBotReplyMode
    val configMap = BotFacade.createConfigMap(
        mapOf(
            jiraBrowseIssueBaseUrl to validateOptionUrl(cmdLine, "jia"),
            jiraRestServiceBaseUrl to validateOptionUrl(cmdLine, "jrs", false),
            jiraRestServiceAuthUser to jiraUser,
            jiraRestServiceAuthPassword to jiraPassword,
            jiraReferenceBotReplyMode to jiraBotRepliesMode,
            clientProxyHost to proxy[0],
            clientProxyPort to proxy[1]
        )
    )

    runBot(botToken, appToken, configMap, jiraBotRepliesFormat)
}

private fun extractProxy(cmdLine: CommandLine): List<String> {
    cmdLine.getOptionValue("p")?.let {
        it.split(":", limit = 2).let { tokens ->
            require(tokens.size == 2) { "malformed proxy" }
            return tokens
        }
    }
    return listOf("", "")
}

private fun extractJiraCredentials(cmdLine: CommandLine): List<String> {
    cmdLine.getOptionValue("u")?.let {
        it.split(":", limit = 2).let { tokens ->
            if (tokens.size == 2)
                return tokens
        }
    }
    return listOf("", "")
}

private fun extractEnumOption(cmdLine: CommandLine, option: String, resolve: (String) -> Enum<*>): Enum<*> {
    requireOption(cmdLine, option)
    val value = cmdLine.getOptionValue(option)
    try {
        return resolve.invoke(value?.toUpperCase() ?: "")
    } catch (t: Throwable) {
        error("Unsupported content for option $option: $value")
    }
}

private fun validateOptionUrl(cmdLine: CommandLine, option: String, mandatory: Boolean = true): String {
    val value = cmdLine.getOptionValue(option)

    if (!mandatory) {
        return if (!value.isNullOrEmpty()) validateUrl(value) else ""
    }

    requireOption(cmdLine, option)
    return validateUrl(value)
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

private fun runBot(botToken: String?, appToken: String?, configMap: Map<String, Any>, jiraBotReplyFormat: JiraBotReplyFormat) {

    val conf = AppConfig()
    conf.singleTeamBotToken = botToken
    val app = App(conf)

    if ((configMap[clientProxyHost] as String).isNotBlank() && (configMap[clientProxyPort] as String).isNotBlank()) {
        logger.info("Using proxy: ${configMap[clientProxyHost]}:${configMap[clientProxyPort]}")
        conf.slack.config.proxyUrl = "http://${configMap[clientProxyHost]}:${configMap[clientProxyPort]}"
    }

    logger.info("Using bot configuration: ${configMap.toList().filter { it.first != jiraRestServiceAuthPassword }.toMap()}")
    if ((configMap[jiraRestServiceBaseUrl] as String).isEmpty() || (configMap[jiraRestServiceAuthUser] as String).isEmpty()) {
        logger.info("Jira resolution with API is disabled!")
    }

    val kodein = Kodein {
        import(getInjectionBindings(configMap))
    }

    val bot = BotFacade(kodein, 10, object : AppEventHandler {
        override fun onJiraResolvedEvent(event: JiraResolvedEvent, kodein: Kodein) {
            val preparedMessage: List<ChatPostMessageRequest> = createSlackMessageFromEvent(event, configMap, jiraBotReplyFormat)
            preparedMessage.forEach {
                sendSlackMessage(event.source.sourceId, it)
            }
        }

        fun sendSlackMessage(channel: String, chatPostMessageRequest: ChatPostMessageRequest): ChatPostMessageResponse {
            chatPostMessageRequest.channel = channel
            return app.client.chatPostMessage(chatPostMessageRequest)
        }
    })

    app.event(MessageEvent::class.java, MessageListener(bot))
    app.event(MessageChangedEvent::class.java, MessageChangedListener(bot))

    SocketModeApp(appToken, app).startAsync()
    logger.info("Session connected")
    Thread.sleep(Long.MAX_VALUE)
}

