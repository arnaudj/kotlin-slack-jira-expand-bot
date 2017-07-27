import com.github.arnaudj.linkify.slackbot.BotFacade
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class JiraLinkHandlerTest {
    val jiraHostBaseUrl1 = "http://localhost/test-jira"
    lateinit var bot: BotFacade
    val replies = mutableListOf<String>()

    @Before
    fun setup() {
        bot = BotFacade(jiraHostBaseUrl1) { eventReady ->
            val markup = BotFacade.eventToMarkup(eventReady)
            replies.add(markup)
        }
        bot.start()
    }

    @After
    fun tearDown(){
        bot.stop()
    }

    fun receiveChatMessage(message: String, channel: String, user: String) {
        bot.handleMessage(message, channel, user)
        Thread.sleep(500) // TODO RFT to allow test chain to be fully synchronous for testing
    }

    fun assertListEquals(a: List<String>, b: List<String>) {
        println("a: $a")
        println("b: $b")
        Assert.assertArrayEquals(a.toTypedArray(), b.toTypedArray())
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
        assertListEquals(replies, listOf("<$jiraHostBaseUrl1/JIRA-1234|JIRA-1234>"))
    }

    @Test fun `given 2 jira references bot provides 2 jira links`() {
        receiveChatMessage("Could you check JIRA-1234 and prod-42 thanks?", "chan1", "pm1")
        assertListEquals(replies, listOf(
                "<$jiraHostBaseUrl1/JIRA-1234|JIRA-1234>",
                "<$jiraHostBaseUrl1/PROD-42|JPROD-42>"
        ))
    }
}
