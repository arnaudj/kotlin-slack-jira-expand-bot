import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import java.net.Proxy


// TODO Support regex patterns for jiras IDs (and multiple jira project keys too)
// TODO Handle external configuration (jira host, token, watched jira project keys)
// TODO Extract options handling

const val jiraHostBaseUrl = "http://localhost/jira"

fun main(args: Array<String>) {

    val options = Options()
    options.addOption("t", true, "set bot auth token")
    options.addOption("p", true, "http proxy with format host:port")
    options.addOption("h", false, "help")

    val cmdLine = DefaultParser().parse(options, args)
    val token = cmdLine.getOptionValue("t")
    if (cmdLine.hasOption("h") || (token?.length ?: -1) < 5) {
        HelpFormatter().printHelp("bot", options)
        return
    }

    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace")

    val proxy = cmdLine.getOptionValue("p")
    val session =
            if (proxy == null)
                SlackSessionFactory.createWebSocketSlackSession(token)
            else
                SlackSessionFactory.getSlackSessionBuilder(token).apply {
                    withAutoreconnectOnDisconnection(true)

                    println("Using proxy $proxy")
                    val elmt = proxy.split(":", limit = 2)
                    require(elmt.size == 2, { "malformed proxy" })
                    withProxy(Proxy.Type.HTTP, elmt[0], elmt[1].toInt())
                }.build()

    session.connect()
    val channel = session.findChannelByName("general") // make sure bot is a member of the channel, else send will be rejected
    session.sendMessage(channel, "hi im a bot")

    session.addMessagePostedListener(SlackMessagePostedListener { event, session ->
        //if (event.channel.id != session.findChannelByName("thechannel").id) return // target per channel
        //if (event.sender.id != session.findUserByUserName("gueststar").id) return // target per user
        if (session.sessionPersona().id == event.sender.id)
            return@SlackMessagePostedListener // filter own messages, especially not to match own replies indefinitely

        if (event.messageContent.contains("JIRA-1234")) {
            session.sendMessage(event.channel, "<$jiraHostBaseUrl/JIRA-1234|JIRA-1234>")
        }
    })
}

