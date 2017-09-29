import com.github.arnaudj.linkify.spi.jira.restclient.Jira7RestClientImpl
import com.github.arnaudj.linkify.spi.jira.restclient.JiraRestClient
import com.github.salomonbrys.kodein.instance
import org.junit.Assert
import org.junit.Test

class Jira7RestClientImplTest : JiraWithInterceptorTestBase() {

    @Test
    fun `Test JiraRestClient resolution`() {
        setupObjects(true)
        val jiraRestClient = kodein.instance<JiraRestClient>()
        val entity = jiraRestClient.resolve(jiraRestServiceBaseUrl, jiraBrowseIssueBaseUrl, "JIRA-1234")
        Assert.assertEquals("JiraEntity(key=JIRA-1234, jiraIssueBrowseURL=http://localhost/browse, summary=A subtask with some summary here, " +
                "fieldsMap={summary=Some summary here, created=2017-03-17T15:37:10.000+0100, updated=2017-07-17T10:42:55.000+0200, status.name=Closed, " +
                "priority.name=Minor, reporter.name=jdoe, assignee.name=noone})",
                entity.toString())
        Assert.assertEquals("http://localhost/browse/JIRA-1234", entity.getURL())
    }

    @Test
    fun `Test JiraRestClient reply unmarshalling`() {
        setupObjects(true)
        val entity = Jira7RestClientImpl(configMap).decodeEntity("JIRA-1234.mock.json".loadFromResources(), jiraBrowseIssueBaseUrl)
        Assert.assertEquals(
                "JiraEntity(key=JIRA-1234, jiraIssueBrowseURL=http://localhost/browse, summary=A subtask with some summary here, " +
                        "fieldsMap={summary=Some summary here, created=2017-03-17T15:37:10.000+0100, updated=2017-07-17T10:42:55.000+0200, " +
                        "status.name=Closed, priority.name=Minor, reporter.name=jdoe, assignee.name=noone})",
                entity.toString()
        )
    }

}
