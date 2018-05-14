import com.github.arnaudj.linkify.slackbot.AppEventHandler
import com.github.arnaudj.linkify.slackbot.BotFacade
import com.github.arnaudj.linkify.slackbot.SlackbotModule
import com.github.arnaudj.linkify.slackbot.dtos.replies.JiraBotReplyFormat
import com.github.arnaudj.linkify.slackbot.dtos.replies.JiraBotReplyMode
import com.github.arnaudj.linkify.slackbot.eventdriven.events.JiraResolvedEvent
import com.github.arnaudj.linkify.slackbot.eventdriven.events.JiraSeenEvent
import com.github.arnaudj.linkify.spi.jira.restclient.Jira7RestClientImpl
import com.github.arnaudj.linkify.spi.jira.restclient.JiraRestClient
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.singleton
import com.ullink.slack.simpleslackapi.SlackPreparedMessage
import okhttp3.*

fun String.loadFromResources() = JiraWithInterceptorTestBase::class.java.getResource(this).readText()

open class JiraWithInterceptorTestBase : JiraTestBase() {
    lateinit var bot: BotFacade
    lateinit var kodein: Kodein

    private val mockReplies = mapOf(
            "/rest/api/latest/issue/JIRA-1234" to "JIRA-1234.mock.json".loadFromResources(),
            "/rest/api/latest/issue/PROD-42" to "PROD-42.mock.json".loadFromResources()
    )
    val replies = mutableListOf<SlackPreparedMessage>()

    fun setupObjects(jiraResolveWithAPI: Boolean, jiraBotReplyMode: JiraBotReplyMode = JiraBotReplyMode.INLINE) {
        setupConfigMap(jiraResolveWithAPI, jiraBotReplyMode)
        kodein = Kodein {
            import(SlackbotModule.getInjectionBindings(configMap))
            if (jiraResolveWithAPI)
                bind<JiraRestClient>(overrides = true) with singleton { StubbedJiraRestClient(mockReplies, configMap) }
        }

        bot = BotFacade(kodein, -1, object : AppEventHandler {
            override fun onJiraSeenEvent(event: JiraSeenEvent, bot: BotFacade, kodein: Kodein) {
                doDefaultOnJiraSeenEvent(event, bot, kodein)
            }

            override fun onJiraResolvedEvent(event: JiraResolvedEvent, bot: BotFacade, kodein: Kodein) {
                BotFacade.createSlackMessageFromEvent(event, configMap, JiraBotReplyFormat.SHORT).forEach {
                    replies.add(it)
                }
            }
        })

    }
}


private class StubbedJiraRestClient(
        private val mockReplies: Map<String, String>,
        configMap: Map<String, Any>
) : Jira7RestClientImpl(configMap), JiraRestClient {

    override fun createClientBuilder(): okhttp3.OkHttpClient.Builder = with(super.createClientBuilder()) {
        // https://github.com/square/okhttp/wiki/Interceptors
        this.addInterceptor { chain: Interceptor.Chain ->
            val req = chain.request()

            assert(Credentials.basic(jiraAuthUser, jiraAuthPwd) == req.header("Authorization"), { "Invalid Credentials" })

            val rawReply = mockReplies[req.url().encodedPath()]
            val reply = rawReply ?: error("No stub found for this URL")
            Response.Builder()
                    .code(200)
                    .message("msg here")
                    .request(chain.request())
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .body(ResponseBody.create(MediaType.parse("application/json"), reply.toByteArray()))
                    .addHeader("content-type", "application/json")
                    .build()
        }
    }
}
