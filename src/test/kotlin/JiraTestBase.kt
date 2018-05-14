import com.github.arnaudj.linkify.config.ConfigurationConstants
import com.github.arnaudj.linkify.slackbot.BotFacade
import com.github.arnaudj.linkify.slackbot.dtos.replies.JiraBotReplyMode

open class JiraTestBase {
    val jiraBrowseIssueBaseUrl = "http://localhost/browse"
    val jiraRestServiceBaseUrl = "http://localhost.test"
    val jiraAuthUser = "someuser"
    val jiraAuthPwd = "somepwd"

    lateinit var configMap: Map<String, Any>

    fun setupConfigMap(jiraResolveWithAPI: Boolean, jiraJiraBotBotReplyMode: JiraBotReplyMode) {
        configMap = BotFacade.createConfigMap(mapOf(
                ConfigurationConstants.jiraBrowseIssueBaseUrl to jiraBrowseIssueBaseUrl,
                ConfigurationConstants.jiraRestServiceBaseUrl to jiraRestServiceBaseUrl,
                ConfigurationConstants.jiraRestServiceAuthUser to if (jiraResolveWithAPI) jiraAuthUser else "",
                ConfigurationConstants.jiraRestServiceAuthPassword to if (jiraResolveWithAPI) jiraAuthPwd else "",
                ConfigurationConstants.jiraReferenceBotReplyMode to jiraJiraBotBotReplyMode
        ))
    }
}
