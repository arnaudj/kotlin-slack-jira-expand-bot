import com.github.arnaudj.linkify.engines.jira.AppEventHandler
import com.github.arnaudj.linkify.engines.jira.entities.JiraBotReplyFormat
import com.github.arnaudj.linkify.engines.jira.entities.JiraBotReplyMode
import com.github.arnaudj.linkify.engines.jira.entities.JiraResolvedEvent
import com.github.arnaudj.linkify.slackbot.BotFacade
import com.github.arnaudj.linkify.slackbot.BotFacade.Companion.createSlackMessageFromEvent
import com.github.arnaudj.linkify.slackbot.SlackbotModule.Companion.getInjectionBindings
import com.github.arnaudj.linkify.spi.jira.restclient.Jira7RestClientImpl
import com.github.arnaudj.linkify.spi.jira.restclient.JiraRestClient
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.singleton
import com.ullink.slack.simpleslackapi.SlackPreparedMessage
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull

fun String.loadFromResources() = JiraWithInterceptorTestBase::class.java.getResource(this).readText()

open class JiraWithInterceptorTestBase : JiraTestBase() {
    lateinit var bot: BotFacade
    lateinit var kodein: Kodein

    protected val mockReplyJira1234 = "JIRA-1234.mock.json".loadFromResources()
    protected val mockReplyProd42 = "PROD-42.mock.json".loadFromResources()

    private val mockReplies = mapOf(
            "/rest/api/latest/issue/JIRA-1234" to mockReplyJira1234,
            "/rest/api/latest/issue/PROD-42" to mockReplyProd42
    )
    val replies = mutableListOf<SlackPreparedMessage>()

    fun setupObjects(jiraResolveWithAPI: Boolean, jiraBotReplyMode: JiraBotReplyMode = JiraBotReplyMode.INLINE) {
        setupConfigMap(jiraResolveWithAPI, jiraBotReplyMode)
        kodein = Kodein {
            import(getInjectionBindings(configMap))
            if (jiraResolveWithAPI)
                bind<JiraRestClient>(overrides = true) with singleton { StubbedJiraRestClient(mockReplies, configMap) }
        }

        bot = BotFacade(kodein, -1, object : AppEventHandler {
            override fun onJiraResolvedEvent(event: JiraResolvedEvent, kodein: Kodein) {
                val preparedMessage: List<SlackPreparedMessage> = createSlackMessageFromEvent(event, configMap, JiraBotReplyFormat.SHORT)
                preparedMessage.forEach {
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

            val rawReply = mockReplies[req.url.encodedPath]
            val reply = rawReply ?: error("No stub found for this URL")
            Response.Builder()
                    .code(200)
                    .message("msg here")
                    .request(chain.request())
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .body(ResponseBody.create("application/json".toMediaTypeOrNull(), reply.toByteArray()))
                    .addHeader("content-type", "application/json")
                    .build()
        }
    }
}
