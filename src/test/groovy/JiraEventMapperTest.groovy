import com.github.arnaudj.linkify.engines.jira.ConfigurationConstants
import com.github.arnaudj.linkify.engines.jira.entities.JiraBotReplyFormat
import com.github.arnaudj.linkify.engines.jira.entities.JiraBotReplyMode
import com.github.arnaudj.linkify.engines.jira.entities.JiraEntity
import com.github.arnaudj.linkify.engines.jira.entities.JiraResolvedEvent
import com.github.arnaudj.linkify.eventdriven.events.EventSourceData
import com.github.arnaudj.linkify.slackbot.BotFacade
import com.slack.api.methods.request.chat.ChatPostMessageRequest
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
        def chatPostMessageRequest = subTestExtendedReplyFormat(event)

        then:
        expectedReplythreadTimestamp == chatPostMessageRequest.threadTs

        where:
        replyMode               | sourceMessageThreadTimestamp | expectedReplythreadTimestamp
        JiraBotReplyMode.INLINE | null                         | null
        JiraBotReplyMode.INLINE | "ts0"                        | "ts0" // follow thread

        JiraBotReplyMode.THREAD | "ts0"                        | "ts0" // follow thread
        JiraBotReplyMode.THREAD | null                         | "ts1" // start thread
    }

    private ChatPostMessageRequest subTestExtendedReplyFormat(JiraResolvedEvent event) {
        def preparedMessages = BotFacade.createSlackMessageFromEvent(event, configMap, jiraBotReplyFormatExtended)
        assert 1 == preparedMessages.size()
        def pm = preparedMessages[0]
        assert 1 == pm.attachments.size()
        with(pm.attachments[0]) {
            assert "JIRA-1234: A subtask with some summary here" == title
            assert "A subtask with some summary here" == fallback
            assert null == text
            assert null == pretext
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
        def chatPostMessageRequest = subTestShortReplyFormat(event)

        then:
        expectedReplythreadTimestamp == chatPostMessageRequest.threadTs

        where:
        replyMode               | sourceMessageThreadTimestamp | expectedReplythreadTimestamp
        JiraBotReplyMode.INLINE | null                         | null
        JiraBotReplyMode.INLINE | "ts0"                        | "ts0" // follow thread

        JiraBotReplyMode.THREAD | "ts0"                        | "ts0" // follow thread
        JiraBotReplyMode.THREAD | null                         | "ts1" // start thread
    }

    private ChatPostMessageRequest subTestShortReplyFormat(JiraResolvedEvent event) {
        def preparedMessages = BotFacade.createSlackMessageFromEvent(event, configMap, jiraBotReplyFormatShort)
        assert 1 == preparedMessages.size()
        def pm = preparedMessages[0]
        assert "<$jiraBrowseIssueBaseUrl/JIRA-1234|JIRA-1234> `A subtask with some summary here`" == pm.text
        assert null == pm.attachments
        return pm
    }

    def expandFields(fields) {
        fields.collect {
            "${it.title}=${it.value}"
        }.toString()
    }
}
