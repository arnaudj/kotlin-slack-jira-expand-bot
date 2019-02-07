import com.github.arnaudj.linkify.engines.jira.ConfigurationConstants
import com.github.arnaudj.linkify.engines.jira.entities.JiraBotReplyMode
import com.github.arnaudj.linkify.slackbot.BotFacade.Companion.createConfigMap

open class JiraTestBase {
    val jiraBrowseIssueBaseUrl = "http://localhost/browse"
    val jiraRestServiceBaseUrl = "http://localhost.test"
    val jiraAuthUser = "someuser"
    val jiraAuthPwd = "somepwd"

    lateinit var configMap: Map<String, Any>

    fun setupConfigMap(jiraResolveWithAPI: Boolean, jiraJiraBotBotReplyMode: JiraBotReplyMode) {
        configMap = createConfigMap(mapOf(
                ConfigurationConstants.jiraBrowseIssueBaseUrl to jiraBrowseIssueBaseUrl,
                ConfigurationConstants.jiraRestServiceBaseUrl to jiraRestServiceBaseUrl,
                ConfigurationConstants.jiraRestServiceAuthUser to if (jiraResolveWithAPI) jiraAuthUser else "",
                ConfigurationConstants.jiraRestServiceAuthPassword to if (jiraResolveWithAPI) jiraAuthPwd else "",
                ConfigurationConstants.jiraReferenceBotReplyMode to jiraJiraBotBotReplyMode
        ))
    }
}
