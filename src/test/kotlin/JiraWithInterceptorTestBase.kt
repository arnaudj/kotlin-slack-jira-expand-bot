import com.github.arnaudj.linkify.config.ConfigurationConstants
import com.github.arnaudj.linkify.slackbot.BotFacade
import com.github.arnaudj.linkify.slackbot.SlackbotModule
import com.github.arnaudj.linkify.spi.jira.restclient.Jira7RestClientImpl
import com.github.arnaudj.linkify.spi.jira.restclient.JiraRestClient
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.singleton
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody
import java.util.concurrent.Executor

fun String.loadFromResources() = JiraWithInterceptorTestBase::class.java.getResource(this).readText()

open class JiraWithInterceptorTestBase {
    val jiraBrowseIssueBaseUrl = "http://localhost/browse"
    val jiraRestServiceBaseUrl = "http://localhost.test"

    lateinit var bot: BotFacade
    lateinit var configMap: Map<String, Any>
    lateinit var kodein: Kodein

    class StubbedJiraRestClient : Jira7RestClientImpl(), JiraRestClient {
        val mockReplies = mapOf(
                "/rest/api/latest/issue/JIRA-1234" to "JIRA-1234.mock.json".loadFromResources(),
                "/rest/api/latest/issue/PROD-42" to "PROD-42.mock.json".loadFromResources()
        )

        override fun createClientBuilder(): okhttp3.OkHttpClient.Builder = with(super.createClientBuilder()) {
            // https://github.com/square/okhttp/wiki/Interceptors
            this.addInterceptor { chain: Interceptor.Chain ->
                val req = chain.request()
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

    fun setupObjects(jiraResolveWithAPI: Boolean) {
        configMap = mapOf(
                ConfigurationConstants.jiraBrowseIssueBaseUrl to jiraBrowseIssueBaseUrl,
                ConfigurationConstants.jiraRestServiceBaseUrl to jiraRestServiceBaseUrl,
                ConfigurationConstants.jiraResolveWithAPI to jiraResolveWithAPI
        )

        kodein = Kodein {
            import(SlackbotModule.getInjectionBindings(configMap))
            if (jiraResolveWithAPI)
                bind<JiraRestClient>(overrides = true) with singleton { StubbedJiraRestClient() }
        }

        val currentThreadExecutor = Executor { it.run() }
        bot = BotFacade(currentThreadExecutor, kodein)
    }
}
