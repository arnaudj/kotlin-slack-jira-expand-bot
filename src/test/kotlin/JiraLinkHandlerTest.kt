import com.ullink.slack.simpleslackapi.SlackPreparedMessage
import org.junit.Assert
import org.junit.Test


class JiraLinkHandlerTest : JiraWithInterceptorTestBase() {
    fun receiveChatMessage(message: String, channel: String, user: String) {
        bot.handleChatMessage(message, channel, user)
    }

    @Test
    fun `(with jira API) given no jira reference bot says nothing`() {
        setupObjects(true)
        receiveChatMessage("A normal conversation message", "chan1", "user1")
        Assert.assertTrue(replies.isEmpty())
    }

    @Test
    fun `(no jira API) given no jira reference bot says nothing`() {
        setupObjects(false)
        receiveChatMessage("A normal conversation message", "chan1", "user1")
        Assert.assertTrue(replies.isEmpty())
    }

    @Test
    fun `(with jira API) given malformed jira reference bot says nothing`() {
        setupObjects(true)
        `given malformed jira reference bot says nothing`()
    }

    @Test
    fun `(no jira API) given malformed jira reference bot says nothing`() {
        setupObjects(false)
        `given malformed jira reference bot says nothing`()
    }

    private fun `given malformed jira reference bot says nothing`() {
        receiveChatMessage("DEC-2050", "chan1", "user1")
        Assert.assertTrue(replies.isEmpty())

        receiveChatMessage("dec-2050", "chan1", "user1")
        Assert.assertTrue(replies.isEmpty())

        receiveChatMessage("DEC - 2017", "chan1", "user1")
        Assert.assertTrue(replies.isEmpty())

        receiveChatMessage("2017-2020", "chan1", "user1")
        Assert.assertTrue(replies.isEmpty())
    }

    fun buildMessage(message: String): SlackPreparedMessage = SlackPreparedMessage.Builder().withMessage(message).build()
    fun buildMessages(vararg messages: String): List<SlackPreparedMessage> = messages.map { buildMessage(it) }.toList()

    @Test
    fun `(with jira API) given 1 jira reference bot provides 1 jira link`() {
        setupObjects(true)
        receiveChatMessage("Could you check JIRA-1234?", "chan1", "pm1")
        assertMessagesEquals(buildMessages("<$jiraBrowseIssueBaseUrl/JIRA-1234|JIRA-1234> `A subtask with some summary here`"), replies)
    }

    @Test
    fun `(no jira API) given 1 jira reference bot provides 1 jira link`() {
        setupObjects(false)
        receiveChatMessage("Could you check JIRA-1234?", "chan1", "pm1")
        assertMessagesEquals(buildMessages("<$jiraBrowseIssueBaseUrl/JIRA-1234|JIRA-1234>"), replies)
    }

    @Test
    fun `(with jira API) given 2 jira references bot provides 2 jira links`() {
        setupObjects(true)
        receiveChatMessage("Could you check JIRA-1234 and prod-42 thanks?", "chan1", "pm1")
        assertMessagesEquals(buildMessages("<$jiraBrowseIssueBaseUrl/JIRA-1234|JIRA-1234> `A subtask with some summary here`",
                "<$jiraBrowseIssueBaseUrl/PROD-42|PROD-42> `Another summary here available in fields`"), replies)
    }

    @Test
    fun `(no jira API) given 2 jira references bot provides 2 jira links`() {
        setupObjects(false)
        receiveChatMessage("Could you check JIRA-1234 and prod-42 thanks?", "chan1", "pm1")
        assertMessagesEquals(buildMessages("<$jiraBrowseIssueBaseUrl/JIRA-1234|JIRA-1234>",
                "<$jiraBrowseIssueBaseUrl/PROD-42|PROD-42>"), replies)
    }

    fun assertMessagesEquals(a: List<SlackPreparedMessage>, b: List<SlackPreparedMessage>) {
        val a2 = a.map { it.toString() }
        val b2 = b.map { it.toString() }
        Assert.assertArrayEquals(a2.toTypedArray(), b2.toTypedArray())
    }
}
