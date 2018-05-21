import com.github.arnaudj.linkify.config.ConfigurationConstants
import com.github.arnaudj.linkify.eventdriven.events.EventSourceData
import com.github.arnaudj.linkify.slackbot.BotFacade
import com.github.arnaudj.linkify.slackbot.dtos.replies.JiraBotReplyFormat
import com.github.arnaudj.linkify.slackbot.dtos.replies.JiraBotReplyMode
import com.github.arnaudj.linkify.slackbot.eventdriven.events.JiraResolvedEvent
import com.github.arnaudj.linkify.spi.jira.JiraEntity
import com.ullink.slack.simpleslackapi.SlackPreparedMessage
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class JiraEventMapperTest extends Specification {
    def jiraBrowseIssueBaseUrl = "http://localhost/browse"
    def jiraRestServiceBaseUrl = "http://localhost.test"
    def jiraAuthUser = "someuser"
    def jiraAuthPwd = "somepwd"

    def configMap

    def setupConfigMap(boolean jiraResolveWithAPI, JiraBotReplyMode jiraJiraBotBotReplyMode) {
        configMap = BotFacade.createConfigMap([
                (ConfigurationConstants.jiraBrowseIssueBaseUrl)     : jiraBrowseIssueBaseUrl,
                (ConfigurationConstants.jiraRestServiceBaseUrl)     : jiraRestServiceBaseUrl,
                (ConfigurationConstants.jiraRestServiceAuthUser)    : (jiraResolveWithAPI) ? jiraAuthUser : "",
                (ConfigurationConstants.jiraRestServiceAuthPassword): (jiraResolveWithAPI) ? jiraAuthPwd : "",
                (ConfigurationConstants.jiraReferenceBotReplyMode)  : jiraJiraBotBotReplyMode
        ])
    }


    def jiraBotReplyFormatExtended = JiraBotReplyFormat.EXTENDED
    def jiraBotReplyFormatShort = JiraBotReplyFormat.SHORT

    def jiraEntity1 = new JiraEntity(
            "JIRA-1234",
            "http://localhost/browse",
            "A subtask with some summary here",
            ["summary"      : "Some summary here",
             "created"      : "2017-03-17T15:37:10.000+0100",
             "updated"      : "2017-07-17T10:42:55.000+0200",
             "status.name"  : "Closed",
             "priority.name": "Minor",
             "reporter.name": "jdoe",
             "assignee.name": "noone"
            ]
    )

    def "(extended reply format)(#replyMode) map event with thread_ts #sourceMessageThreadTimestamp expected thread_ts #expectedReplythreadTimestamp"() {
        given:
        setupConfigMap(false, replyMode)

        when:
        def event = new JiraResolvedEvent(new EventSourceData("sid1", "uuid1", "ts1", sourceMessageThreadTimestamp), jiraEntity1)
        def slackPreparedMessage = subTestExtendedReplyFormat(event)

        then:
        expectedReplythreadTimestamp == slackPreparedMessage.threadTimestamp

        where:
        replyMode               | sourceMessageThreadTimestamp | expectedReplythreadTimestamp
        JiraBotReplyMode.INLINE | null                         | null
        JiraBotReplyMode.INLINE | "ts0"                        | "ts0" // follow thread

        JiraBotReplyMode.THREAD | "ts0"                        | "ts0" // follow thread
        JiraBotReplyMode.THREAD | null                         | "ts1" // start thread
    }

    private SlackPreparedMessage subTestExtendedReplyFormat(JiraResolvedEvent event) {
        def preparedMessages = BotFacade.createSlackMessageFromEvent(event, configMap, jiraBotReplyFormatExtended)
        assert 1 == preparedMessages.size()
        def pm = preparedMessages[0]
        assert 1 == pm.attachments.size()
        with(pm.attachments[0]) {
            assert "JIRA-1234: A subtask with some summary here" == title
            assert "A subtask with some summary here" == fallback
            assert "" == text
            assert "" == pretext
            assert "<!date^1500280975^Updated: {date_num} {time_secs}|2017-07-17T10:42:55.000+0200>" == footer
            assert "[Priority=Minor, Status=Closed, Reporter=jdoe, Assignee=noone]" == expandFields(fields)
        }

        return pm
    }

    def "(short reply format)(#replyMode) map event with thread_ts #sourceMessageThreadTimestamp expected thread_ts #expectedReplythreadTimestamp"() {
        given:
        setupConfigMap(false, replyMode)

        when:
        def event = new JiraResolvedEvent(new EventSourceData("sid1", "uuid1", "ts1", sourceMessageThreadTimestamp), jiraEntity1)
        def slackPreparedMessage = subTestShortReplyFormat(event)

        then:
        expectedReplythreadTimestamp == slackPreparedMessage.threadTimestamp

        where:
        replyMode               | sourceMessageThreadTimestamp | expectedReplythreadTimestamp
        JiraBotReplyMode.INLINE | null                         | null
        JiraBotReplyMode.INLINE | "ts0"                        | "ts0" // follow thread

        JiraBotReplyMode.THREAD | "ts0"                        | "ts0" // follow thread
        JiraBotReplyMode.THREAD | null                         | "ts1" // start thread
    }

    private SlackPreparedMessage subTestShortReplyFormat(JiraResolvedEvent event) {
        def preparedMessages = BotFacade.createSlackMessageFromEvent(event, configMap, jiraBotReplyFormatShort)
        assert 1 == preparedMessages.size()
        def pm = preparedMessages[0]
        assert "<$jiraBrowseIssueBaseUrl/JIRA-1234|JIRA-1234> `A subtask with some summary here`" == pm.message
        assert 0 == pm.attachments.size()
        return pm
    }

    def expandFields(fields) {
        fields.collect {
            "${it.title}=${it.value}"
        }.toString()
    }
}
