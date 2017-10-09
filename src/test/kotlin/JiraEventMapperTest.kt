import com.github.arnaudj.linkify.slackbot.BotFacade
import com.github.arnaudj.linkify.slackbot.cqrs.events.JiraResolved
import com.github.arnaudj.linkify.slackbot.cqrs.mappers.JiraBotReplyFormat
import com.github.arnaudj.linkify.spi.jira.JiraEntity
import com.ullink.slack.simpleslackapi.SlackField
import org.junit.Assert
import org.junit.Test

class JiraEventMapperTest : JiraWithInterceptorTestBase() {
    val jiraBotReplyFormatExtended = JiraBotReplyFormat.EXTENDED
    val jiraEntity1 = JiraEntity(key = "JIRA-1234", jiraIssueBrowseURL = "http://localhost/browse", summary = "A subtask with some summary here",
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
    fun `(extended reply format) Test event mapper`() {
        setupConfigMap(jiraResolveWithAPI = false)
        val event = JiraResolved("uuid1", listOf(jiraEntity1))
        val preparedMessages = BotFacade.createSlackMessageFromEvent(event, configMap, jiraBotReplyFormatExtended)

        Assert.assertEquals(1, preparedMessages.size)
        val pm = preparedMessages[0]

        Assert.assertEquals(1, pm.attachments.size)
        val pmatt1 = pm.attachments[0]
        Assert.assertEquals("JIRA-1234: A subtask with some summary here", pmatt1.title)
        Assert.assertEquals("A subtask with some summary here", pmatt1.fallback)
        Assert.assertEquals("", pmatt1.text)
        Assert.assertEquals("", pmatt1.pretext)
        Assert.assertEquals("<!date^1500280975^Updated: {date_num} {time_secs}|2017-07-17T10:42:55.000+0200>", pmatt1.footer)
        Assert.assertEquals("[Priority=Minor, Status=Closed, Reporter=jdoe, Assignee=noone]", expandFields(pmatt1.fields))

    }

    fun expandFields(fields: List<SlackField>) = fields.map { "${it.title}=${it.value}" }.toString()
}
