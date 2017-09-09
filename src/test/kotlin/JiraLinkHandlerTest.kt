import com.github.arnaudj.linkify.config.ConfigurationConstants
import com.github.arnaudj.linkify.slackbot.BotFacade
import com.github.arnaudj.linkify.slackbot.SlackbotModule
import com.github.arnaudj.linkify.spi.jira.JiraEntity
import com.github.arnaudj.linkify.spi.jira.restclient.JiraRestClient
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.singleton
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

class JiraLinkHandlerTest {
    lateinit var bot: BotFacade
    val replies = mutableListOf<String>()
    val singleExecutorService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    val jiraHostBaseUrl1 = "http://localhost/test-jira"
    val configMap = mapOf(
            ConfigurationConstants.jiraHostBaseUrl to jiraHostBaseUrl1,
            ConfigurationConstants.jiraResolveWithAPI to true
    )

    class DummyJira7RestClientImpl : JiraRestClient { // For Jira 7.2.x
        override fun resolve(jiraId: String): JiraEntity {
            TODO("DummyJira7RestClientImpl not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    @Before
    fun setup() {
        val kodein = Kodein {
            import(SlackbotModule.getInjectionBindings(configMap))
            bind<JiraRestClient>(overrides = true) with singleton { DummyJira7RestClientImpl() }
        }
        bot = BotFacade(singleExecutorService, kodein)
    }

    @After
    fun tearDown(){
        singleExecutorService.shutdownNow()
    }

    fun receiveChatMessage(message: String, channel: String, user: String) {
        bot.handleMessage(message, channel, user)
        bot.handleEvents {
            val markup = BotFacade.eventToMarkup(it, configMap)
            replies.add(markup)
        }
    }

    @Test fun `given no jira reference bot says nothing`() {
        receiveChatMessage("A normal conversation message", "chan1", "user1")
        Assert.assertTrue(replies.isEmpty())
    }

    @Test fun `given malformed jira reference bot says nothing`() {
        receiveChatMessage("DEC-2050", "chan1", "user1")
        Assert.assertTrue(replies.isEmpty())

        receiveChatMessage("dec-2050", "chan1", "user1")
        Assert.assertTrue(replies.isEmpty())

        receiveChatMessage("DEC - 2017", "chan1", "user1")
        Assert.assertTrue(replies.isEmpty())

        receiveChatMessage("2017-2020", "chan1", "user1")
        Assert.assertTrue(replies.isEmpty())
    }

    @Test fun `given 1 jira reference bot provides 1 jira link`() {
        receiveChatMessage("Could you check JIRA-1234?", "chan1", "pm1")
        assertListEquals(listOf("<$jiraHostBaseUrl1/JIRA-1234|JIRA-1234>"), replies)
    }

    @Test fun `given 2 jira references bot provides 2 jira links`() {
        receiveChatMessage("Could you check JIRA-1234 and prod-42 thanks?", "chan1", "pm1")
        assertListEquals(listOf("<$jiraHostBaseUrl1/JIRA-1234|JIRA-1234>\n<$jiraHostBaseUrl1/PROD-42|PROD-42>"), replies)
    }

    fun assertListEquals(a: List<String>, b: List<String>) {
        Assert.assertArrayEquals(a.toTypedArray(), b.toTypedArray())
    }
}
