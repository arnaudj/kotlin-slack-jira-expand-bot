package com.github.arnaudj.linkify.slackbot

import com.github.arnaudj.linkify.eventdriven.events.EventSourceData
import com.github.arnaudj.linkify.engines.jira.ConfigurationConstants.jiraBrowseIssueBaseUrl
import com.github.arnaudj.linkify.engines.jira.ConfigurationConstants.jiraReferenceBotReplyMode
import com.github.arnaudj.linkify.engines.jira.ConfigurationConstants.jiraRestServiceAuthPassword
import com.github.arnaudj.linkify.engines.jira.ConfigurationConstants.jiraRestServiceAuthUser
import com.github.arnaudj.linkify.engines.jira.ConfigurationConstants.jiraRestServiceBaseUrl
import com.github.arnaudj.linkify.engines.jira.AppEventHandler
import com.github.arnaudj.linkify.engines.jira.entities.JiraBotReplyFormat
import com.github.arnaudj.linkify.engines.jira.entities.JiraBotReplyMode
import com.github.arnaudj.linkify.engines.jira.entities.JiraResolvedEvent
import com.github.arnaudj.linkify.slackbot.BotFacade.Companion.createSlackMessageFromEvent
import com.github.arnaudj.linkify.slackbot.SlackbotModule.Companion.getInjectionBindings
import com.github.salomonbrys.kodein.Kodein
import com.ullink.slack.simpleslackapi.SlackChannel
import com.ullink.slack.simpleslackapi.SlackMessageHandle
import com.ullink.slack.simpleslackapi.SlackPreparedMessage
import com.ullink.slack.simpleslackapi.SlackSession
import com.ullink.slack.simpleslackapi.events.SlackMessageUpdated
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener
import com.ullink.slack.simpleslackapi.listeners.SlackMessageUpdatedListener
import com.ullink.slack.simpleslackapi.replies.SlackMessageReply
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import org.slf4j.LoggerFactory
import java.net.Proxy

val logger = LoggerFactory.getLogger("SlackBot")

fun main(args: Array<String>) {

    val options = Options()
    options.addOption("t", true, "set bot auth token")
    options.addOption("p", true, "http proxy with format host:port")
    options.addOption("jia", true, "jira http base address for issues browsing (ex: http://jira.nodomain/browse)")
    options.addOption("jrs", true, "jira http base address for rest service (ex: http://jira.nodomain, without '/rest/api/latest/')")
    options.addOption("jfmt", true, "jira bot replies format: ${JiraBotReplyFormat.knownValues()}")
    options.addOption("jmode", true, "jira bot replies mode: ${JiraBotReplyMode.knownValues()}")
    options.addOption("u", true, "jira credentials to resolve issues information, with format user:password")
    options.addOption("h", false, "help")

    val cmdLine = DefaultParser().parse(options, args)
    val token = cmdLine.getOptionValue("t")
    if (cmdLine.hasOption("h") || (token?.length ?: -1) < 5) {
        HelpFormatter().printHelp("bot", options)
        return
    }

    val proxy = cmdLine.getOptionValue("p")
    val (jiraUser, jiraPassword) = extractJiraCredentials(cmdLine)
    val jiraBotRepliesFormat = extractEnumOption(cmdLine, "jfmt", { JiraBotReplyFormat.valueOf(it) }) as JiraBotReplyFormat
    val jiraBotRepliesMode = extractEnumOption(cmdLine, "jmode", { JiraBotReplyMode.valueOf(it) }) as JiraBotReplyMode
    val configMap = BotFacade.createConfigMap(mapOf(
            jiraBrowseIssueBaseUrl to validateOptionUrl(cmdLine, "jia"),
            jiraRestServiceBaseUrl to validateOptionUrl(cmdLine, "jrs", false),
            jiraRestServiceAuthUser to jiraUser,
            jiraRestServiceAuthPassword to jiraPassword,
            jiraReferenceBotReplyMode to jiraBotRepliesMode
    ))

    runBot(token, proxy, configMap, jiraBotRepliesFormat)
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

private fun runBot(token: String?, proxy: String?, configMap: Map<String, Any>, jiraBotReplyFormat: JiraBotReplyFormat) {
    val session = SlackSessionFactory.getSlackSessionBuilder(token).apply {
        withAutoreconnectOnDisconnection(true)

        proxy?.let {
            logger.info("Using proxy: $it")
            val elmt = it.split(":", limit = 2)
            require(elmt.size == 2, { "malformed proxy" })
            withProxy(Proxy.Type.HTTP, elmt[0], elmt[1].toInt())
        }
    }.build()

    logger.info("Using bot configuration: ${configMap.toList().filter { it.first != jiraRestServiceAuthPassword }.toMap()}")
    if ((configMap[jiraRestServiceBaseUrl] as String).isEmpty() || (configMap[jiraRestServiceAuthUser] as String).isEmpty())
        logger.info("Jira resolution with API is disabled!")

    val kodein = Kodein {
        import(getInjectionBindings(configMap))
    }

    val bot = BotFacade(kodein, 10, object : AppEventHandler {
        override fun onJiraResolvedEvent(event: JiraResolvedEvent, kodein: Kodein) {
            val preparedMessage: List<SlackPreparedMessage> = createSlackMessageFromEvent(event, configMap, jiraBotReplyFormat)
            val channel = session.findChannelById(event.source.sourceId)
            preparedMessage.forEach {
                sendSlackMessage(channel, it)
            }
        }

        fun sendSlackMessage(channel: SlackChannel, preparedMessage: SlackPreparedMessage): SlackMessageHandle<SlackMessageReply> {
            return session.sendMessage(channel, preparedMessage)
        }
    })

    session.addMessageUpdatedListener { event: SlackMessageUpdated, _: SlackSession? ->
        with(event) {
            // without persistence, we don't have threadTimestamp, nor user uid
            // messageTimestamp: ts of original message
            // editionTimestamp: ts at edit time
            bot.handleChatMessage(newMessage, EventSourceData(channel.id, "unknownuid", messageTimestamp, messageTimestamp))
        }
    }

    session.addMessagePostedListener(SlackMessagePostedListener { event, _ ->
        //if (event.channelId.id != session.findChannelByName("thechannel").id) return // target per channelId
        //if (event.sender.id != session.findUserByUserName("gueststar").id) return // target per user
        if (session.sessionPersona().id == event.sender.id || event?.user?.isBot == true)
            return@SlackMessagePostedListener // filter own and 3rd party bot messages

        with(event) {
            bot.handleChatMessage(messageContent, EventSourceData(channel.id, user.id, timeStamp, threadTimestamp))
        }
    })

    session.connect()
    logger.info("Session connected")
}

