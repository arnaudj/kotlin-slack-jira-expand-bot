import com.github.arnaudj.linkify.config.ConfigurationConstants

open class JiraTestBase {
    val jiraBrowseIssueBaseUrl = "http://localhost/browse"
    val jiraRestServiceBaseUrl = "http://localhost.test"
    val jiraAuthUser = "someuser"
    val jiraAuthPwd = "somepwd"

    lateinit var configMap: Map<String, Any>

    fun setupConfigMap(jiraResolveWithAPI: Boolean) {
        configMap = mapOf(
                ConfigurationConstants.jiraBrowseIssueBaseUrl to jiraBrowseIssueBaseUrl,
                ConfigurationConstants.jiraRestServiceBaseUrl to jiraRestServiceBaseUrl,
                ConfigurationConstants.jiraRestServiceAuthUser to if (jiraResolveWithAPI) jiraAuthUser else "",
                ConfigurationConstants.jiraRestServiceAuthPassword to if (jiraResolveWithAPI) jiraAuthPwd else ""
        )
    }
}
