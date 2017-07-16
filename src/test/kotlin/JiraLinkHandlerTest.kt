import com.github.arnaudj.JiraLinkHandler
import com.github.arnaudj.dtos.results.ReplyResult
import com.github.arnaudj.dtos.results.noopReply
import com.github.arnaudj.dtos.results.sendMessageReply
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class JiraLinkHandlerTest {
    val jiraHostBaseUrl1 = "http://localhost/test-jira"
    lateinit var logic: JiraLinkHandler

    @Before
    fun setup() {
        logic = JiraLinkHandler(jiraHostBaseUrl1)
    }

    fun assertListEquals(a: List<ReplyResult>, b: List<ReplyResult>) {
        Assert.assertArrayEquals(a.toTypedArray(), b.toTypedArray())
    }

    @Test fun `given no jira reference bot says nothing`() {
        val reply = logic.handleMessage("A normal conversation message", "chan1", "user1")
        assertListEquals(listOf(noopReply), reply)
    }

    @Test fun `given malformed jira reference bot says nothing`() {
        assertListEquals(listOf(noopReply), logic.handleMessage("DEC-2050", "chan1", "user1"))
        assertListEquals(listOf(noopReply), logic.handleMessage("dec-2050", "chan1", "user1"))
        assertListEquals(listOf(noopReply), logic.handleMessage("DEC - 2017", "chan1", "user1"))
        assertListEquals(listOf(noopReply), logic.handleMessage("2017-2020", "chan1", "user1"))
    }

    @Test fun `given 1 jira reference bot provides 1 jira link`() {
        val reply = logic.handleMessage("Could you check JIRA-1234?", "chan1", "pm1")
        assertListEquals(
                listOf(ReplyResult(sendMessageReply.action, "chan1", "<$jiraHostBaseUrl1/JIRA-1234|JIRA-1234>")),
                reply)
    }

    @Test fun `given 2 jira references bot provides 2 jira links`() {
        val reply = logic.handleMessage("Could you check JIRA-1234 and prod-42 thanks", "chan1", "pm1")
        Assert.assertEquals(
                listOf(
                        ReplyResult(sendMessageReply.action, "chan1", "<$jiraHostBaseUrl1/JIRA-1234|JIRA-1234>"),
                        ReplyResult(sendMessageReply.action, "chan1", "<$jiraHostBaseUrl1/PROD-42|PROD-42>")
                ),
                reply)
    }
}