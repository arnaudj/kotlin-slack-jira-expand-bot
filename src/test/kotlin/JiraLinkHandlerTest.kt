import com.github.arnaudj.linkify.eventdriven.events.EventSourceData
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import org.junit.Assert
import org.junit.Test


class JiraLinkHandlerTest : JiraWithInterceptorTestBase() {
    val expectedReplyNoJiraAPI_JIRA1234 = "<$jiraBrowseIssueBaseUrl/JIRA-1234|JIRA-1234>"
    val expectedReplyWithJiraAPI_JIRA1234 = "$expectedReplyNoJiraAPI_JIRA1234 `A subtask with some summary here`"
    val expectedReplyNoJiraAPI_PROD42 = "<$jiraBrowseIssueBaseUrl/PROD-42|PROD-42>"
    val expectdReplyWithJiraAPI_PROD42 = "$expectedReplyNoJiraAPI_PROD42 `Another summary here available in fields`"

    fun receiveChatMessage(message: String, channel: String, user: String) {
        bot.handleChatMessage(message, EventSourceData(channel, user, "fakeTimestamp", null))
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
        receiveChatMessage("1900-BC", "chan1", "user1")
        Assert.assertTrue(replies.isEmpty())

        receiveChatMessage("DEC - 2017", "chan1", "user1")
        Assert.assertTrue(replies.isEmpty())

        receiveChatMessage("2019-2020", "chan1", "user1")
        Assert.assertTrue(replies.isEmpty())
    }

    fun buildMessage(message: String): ChatPostMessageRequest = ChatPostMessageRequest.builder().text(message).build()
    fun buildMessages(vararg messages: String): List<ChatPostMessageRequest> = messages.map { buildMessage(it) }.toList()

    @Test
    fun `(with jira API) given 1 jira reference bot provides 1 jira link`() {
        setupObjects(true)
        receiveChatMessage("Could you check JIRA-1234?", "chan1", "pm1")
        assertMessagesEquals(buildMessages(expectedReplyWithJiraAPI_JIRA1234), replies)
    }

    @Test
    fun `(no jira API) given 1 jira reference bot provides 1 jira link`() {
        setupObjects(false)
        receiveChatMessage("Could you check JIRA-1234?", "chan1", "pm1")
        assertMessagesEquals(buildMessages(expectedReplyNoJiraAPI_JIRA1234), replies)
    }

    @Test
    fun `(with jira API) given 2 jira references bot provides 2 jira links`() {
        setupObjects(true)
        receiveChatMessage("Could you check JIRA-1234 and PROD-42 thanks?", "chan1", "pm1")
        assertMessagesEquals(buildMessages(expectedReplyWithJiraAPI_JIRA1234, expectdReplyWithJiraAPI_PROD42), replies)
    }

    @Test
    fun `(no jira API) given 2 jira references bot provides 2 jira links`() {
        setupObjects(false)
        receiveChatMessage("Could you check JIRA-1234 and PROD-42 thanks?", "chan1", "pm1")
        assertMessagesEquals(buildMessages(expectedReplyNoJiraAPI_JIRA1234, expectedReplyNoJiraAPI_PROD42), replies)
    }

    @Test
    fun `(with jira API, throttling) given 1 jira reference in 2 nearby messages on same channel bot provides only 1 jira link`() {
        setupObjects(true)
        `throttling given 1 jira reference in 2 nearby messages on same channel bot provides only 1 jira link`()
        assertMessagesEquals(buildMessages(expectedReplyWithJiraAPI_JIRA1234), replies)
    }

    @Test
    fun `(no jira API, throttling) given 1 jira reference in 2 nearby messages  on same channel bot provides only 1 jira link`() {
        setupObjects(false)
        `throttling given 1 jira reference in 2 nearby messages on same channel bot provides only 1 jira link`()
        assertMessagesEquals(buildMessages(expectedReplyNoJiraAPI_JIRA1234), replies)
    }

    private fun `throttling given 1 jira reference in 2 nearby messages on same channel bot provides only 1 jira link`() {
        receiveChatMessage("Could you check JIRA-1234?", "chan1", "pm1")
        receiveChatMessage("Could you check JIRA-1234?", "chan1", "pm1")
        receiveChatMessage("Some chat", "chan1", "pm1")
        receiveChatMessage("Could you check JIRA-1234?", "chan1", "pm2")
    }

    @Test
    fun `(with jira API, throttling) given 1 jira reference in 2 nearby messages on 2 different channels bot replies twice`() {
        setupObjects(true)
        `throttling given 1 jira reference in 2 nearby messages on 2 different channels bot replies twice`()
        assertMessagesEquals(buildMessages(expectedReplyWithJiraAPI_JIRA1234, expectedReplyWithJiraAPI_JIRA1234), replies)
    }

    @Test
    fun `(no jira API, throttling) given 1 jira reference in 2 nearby messages on 2 different channels bot replies twice`() {
        setupObjects(false)
        `throttling given 1 jira reference in 2 nearby messages on 2 different channels bot replies twice`()
        assertMessagesEquals(buildMessages(expectedReplyNoJiraAPI_JIRA1234, expectedReplyNoJiraAPI_JIRA1234), replies)
    }

    private fun `throttling given 1 jira reference in 2 nearby messages on 2 different channels bot replies twice`() {
        receiveChatMessage("Could you check JIRA-1234?", "chan1", "pm1")
        receiveChatMessage("Could you check JIRA-1234?", "chan2", "pm1")
        receiveChatMessage("Some chat", "chan1", "pm1")
        receiveChatMessage("Could you check JIRA-1234?", "chan1", "pm2")
    }

    fun assertMessagesEquals(expected: List<ChatPostMessageRequest>, actual: List<ChatPostMessageRequest>) {
        val expected0 = expected.map { it.toString() }
        val actual0 = actual.map { it.toString() }
        val message = "Expected:\n$expected0\n\nActual:\n$actual0"
        Assert.assertArrayEquals(message, expected0.toTypedArray(), actual0.toTypedArray())
    }
}
