import com.github.arnaudj.linkify.eventdriven.events.EventSourceData
import com.github.arnaudj.linkify.slackbot.BotFacade
import com.github.arnaudj.linkify.slackbot.dtos.replies.JiraBotReplyFormat
import com.github.arnaudj.linkify.slackbot.dtos.replies.JiraBotReplyMode
import com.github.arnaudj.linkify.slackbot.eventdriven.events.JiraResolvedEvent
import com.github.arnaudj.linkify.spi.jira.JiraEntity
import com.ullink.slack.simpleslackapi.SlackField
import com.ullink.slack.simpleslackapi.SlackPreparedMessage
import org.junit.Assert
import org.junit.Test

class JiraEventMapperTest : JiraTestBase() {
    val jiraBotReplyFormatExtended = JiraBotReplyFormat.EXTENDED
    val jiraBotReplyFormatShort = JiraBotReplyFormat.SHORT
    val jiraEntity1 = JiraEntity(key = "JIRA-1234",
            jiraIssueBrowseURL = "http://localhost/browse",
            summary = "A subtask with some summary here",
            fieldsMap = mapOf(
                    "summary" to "Some summary here",
                    "created" to "2017-03-17T15:37:10.000+0100",
                    "updated" to "2017-07-17T10:42:55.000+0200",
                    "status.name" to "Closed",
                    "priority.name" to "Minor",
                    "reporter.name" to "jdoe",
                    "assignee.name" to "noone"
            ))

    @Test
    fun `(extended reply format)(inline) map unthreaded event`() {
        setupConfigMap(jiraResolveWithAPI = false, jiraJiraBotBotReplyMode = JiraBotReplyMode.INLINE)
        val sourceMessageThreadTimestamp = null
        val expectedReplythreadTimestamp = null

        val event = JiraResolvedEvent(EventSourceData("sid1", "uuid1", "ts1", sourceMessageThreadTimestamp), jiraEntity1)
        val slackPreparedMessage = subTestExtendedReplyFormat(event)
        Assert.assertEquals(expectedReplythreadTimestamp, slackPreparedMessage.threadTimestamp)
    }

    @Test
    fun `(extended reply format)(inline) map threaded event`() {
        setupConfigMap(jiraResolveWithAPI = false, jiraJiraBotBotReplyMode = JiraBotReplyMode.INLINE)
        val sourceMessageThreadTimestamp = "ts0"
        val expectedReplythreadTimestamp = "ts0" // follow thread

        val event = JiraResolvedEvent(EventSourceData("sid1", "uuid1", "ts1", sourceMessageThreadTimestamp), jiraEntity1)
        val slackPreparedMessage = subTestExtendedReplyFormat(event)
        Assert.assertEquals(expectedReplythreadTimestamp, slackPreparedMessage.threadTimestamp)
    }

    @Test
    fun `(extended reply format)(threaded) map unthreaded event`() {
        setupConfigMap(jiraResolveWithAPI = false, jiraJiraBotBotReplyMode = JiraBotReplyMode.THREAD)
        val sourceMessageThreadTimestamp = null
        val expectedReplythreadTimestamp = "ts1" // start thread

        val event = JiraResolvedEvent(EventSourceData("sid1", "uuid1", "ts1", sourceMessageThreadTimestamp), jiraEntity1)
        val slackPreparedMessage = subTestExtendedReplyFormat(event)
        Assert.assertEquals(expectedReplythreadTimestamp, slackPreparedMessage.threadTimestamp)
    }

    @Test
    fun `(extended reply format)(threaded) map threaded event`() {
        setupConfigMap(jiraResolveWithAPI = false, jiraJiraBotBotReplyMode = JiraBotReplyMode.THREAD)
        val sourceMessageThreadTimestamp = "ts0"
        val expectedReplythreadTimestamp = "ts0" // follow thread

        val event = JiraResolvedEvent(EventSourceData("sid1", "uuid1", "ts1", sourceMessageThreadTimestamp), jiraEntity1)
        val slackPreparedMessage = subTestExtendedReplyFormat(event)
        Assert.assertEquals(expectedReplythreadTimestamp, slackPreparedMessage.threadTimestamp)
    }

    private fun subTestExtendedReplyFormat(event: JiraResolvedEvent): SlackPreparedMessage {
        val preparedMessages = BotFacade.createSlackMessageFromEvent(event, configMap, jiraBotReplyFormatExtended)
        Assert.assertEquals(1, preparedMessages.size)
        val pm = preparedMessages[0]
        Assert.assertEquals(1, pm.attachments.size)
        with(pm.attachments[0]) {
            Assert.assertEquals("JIRA-1234: A subtask with some summary here", title)
            Assert.assertEquals("A subtask with some summary here", fallback)
            Assert.assertEquals("", text)
            Assert.assertEquals("", pretext)
            Assert.assertEquals("<!date^1500280975^Updated: {date_num} {time_secs}|2017-07-17T10:42:55.000+0200>", footer)
            Assert.assertEquals("[Priority=Minor, Status=Closed, Reporter=jdoe, Assignee=noone]", expandFields(fields))
        }

        return pm
    }

    @Test
    fun `(short reply format)(inline) map unthreaded event`() {
        setupConfigMap(jiraResolveWithAPI = false, jiraJiraBotBotReplyMode = JiraBotReplyMode.INLINE)
        val sourceMessageThreadTimestamp = null
        val expectedReplythreadTimestamp = null

        val event = JiraResolvedEvent(EventSourceData("sid1", "uuid1", "ts1", sourceMessageThreadTimestamp), jiraEntity1)
        val slackPreparedMessage = subTestShortReplyFormat(event)
        Assert.assertEquals(expectedReplythreadTimestamp, slackPreparedMessage.threadTimestamp)
    }


    @Test
    fun `(short reply format)(inline) map threaded event`() {
        setupConfigMap(jiraResolveWithAPI = false, jiraJiraBotBotReplyMode = JiraBotReplyMode.INLINE)
        val sourceMessageThreadTimestamp = "ts0"
        val expectedReplythreadTimestamp = "ts0" // follow thread

        val event = JiraResolvedEvent(EventSourceData("sid1", "uuid1", "ts1", sourceMessageThreadTimestamp), jiraEntity1)
        val slackPreparedMessage = subTestShortReplyFormat(event)
        Assert.assertEquals(expectedReplythreadTimestamp, slackPreparedMessage.threadTimestamp)
    }

    @Test
    fun `(short reply format)(threaded) map unthreaded event`() {
        setupConfigMap(jiraResolveWithAPI = false, jiraJiraBotBotReplyMode = JiraBotReplyMode.THREAD)
        val sourceMessageThreadTimestamp = null
        val expectedReplythreadTimestamp = "ts1" // start thread

        val event = JiraResolvedEvent(EventSourceData("sid1", "uuid1", "ts1", sourceMessageThreadTimestamp), jiraEntity1)
        val slackPreparedMessage = subTestShortReplyFormat(event)
        Assert.assertEquals(expectedReplythreadTimestamp, slackPreparedMessage.threadTimestamp)
    }

    @Test
    fun `(short reply format)(threaded) map threaded event`() {
        setupConfigMap(jiraResolveWithAPI = false, jiraJiraBotBotReplyMode = JiraBotReplyMode.THREAD)
        val sourceMessageThreadTimestamp = "ts0"
        val expectedReplythreadTimestamp = "ts0" // follow thread

        val event = JiraResolvedEvent(EventSourceData("sid1", "uuid1", "ts1", sourceMessageThreadTimestamp), jiraEntity1)
        val slackPreparedMessage = subTestShortReplyFormat(event)
        Assert.assertEquals(expectedReplythreadTimestamp, slackPreparedMessage.threadTimestamp)
    }

    private fun subTestShortReplyFormat(event: JiraResolvedEvent): SlackPreparedMessage {
        val preparedMessages = BotFacade.createSlackMessageFromEvent(event, configMap, jiraBotReplyFormatShort)
        Assert.assertEquals(1, preparedMessages.size)
        val pm = preparedMessages[0]
        Assert.assertEquals("<$jiraBrowseIssueBaseUrl/JIRA-1234|JIRA-1234> `A subtask with some summary here`", pm.message)
        Assert.assertEquals(0, pm.attachments.size)
        return pm
    }

    fun expandFields(fields: List<SlackField>) = fields.map { "${it.title}=${it.value}" }.toString()
}
