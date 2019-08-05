import com.github.arnaudj.linkify.engines.jira.ConfigurationConstants
import com.github.arnaudj.linkify.engines.jira.entities.JiraBotReplyMode
import com.github.arnaudj.linkify.spi.jira.restclient.Jira7RestClientImpl
import com.github.arnaudj.linkify.spi.jira.restclient.JiraRestClient
import com.github.salomonbrys.kodein.instance
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Jira7RestClientImplTest : JiraWithInterceptorTestBase() {

    @Test
    fun `Test JiraRestClient resolution`() {
        setupObjects(true)
        val jiraRestClient = kodein.instance<JiraRestClient>()
        val entity = jiraRestClient.resolve(jiraRestServiceBaseUrl, jiraBrowseIssueBaseUrl, "JIRA-1234")
        assertEquals("JiraEntity(key=JIRA-1234, jiraIssueBrowseURL=http://localhost/browse, summary=A subtask with some summary here, " +
                "fieldsMap={summary=Some summary here, created=2017-03-17T15:37:10.000+0100, updated=2017-07-17T10:42:55.000+0200, status.name=Closed, " +
                "priority.name=Minor, reporter.name=jdoe, assignee.name=noone})",
                entity.toString())
    }

    @Test
    fun `Test JiraRestClient reply unmarshalling`() {
        setupObjects(true)
        val entity = Jira7RestClientImpl(configMap).decodeEntity("JIRA-1234.mock.json".loadFromResources(), jiraBrowseIssueBaseUrl)
        assertEquals(
                "JiraEntity(key=JIRA-1234, jiraIssueBrowseURL=http://localhost/browse, summary=A subtask with some summary here, " +
                        "fieldsMap={summary=Some summary here, created=2017-03-17T15:37:10.000+0100, updated=2017-07-17T10:42:55.000+0200, " +
                        "status.name=Closed, priority.name=Minor, reporter.name=jdoe, assignee.name=noone})",
                entity.toString()
        )
    }

    @Test
    fun `Test JiraRestClient resolution retries with mock server`() {
        // test ability to overcome http 401 reply
        val server = MockWebServer()

        val authFailed = MockResponse().setResponseCode(401)
                .addHeader("WWW-Authenticate", "Basic")
                .removeHeader("Set-Cookie")
                .removeHeader("Cookie")
        val apiReplyJira1234 = MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Set-Cookie", "JSESSIONID=fake1")
                .setBody(mockReplyJira1234)
        val apiReplyProd42 = MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Set-Cookie", "JSESSIONID=fake2")
                .setBody(mockReplyProd42)

        server.enqueue(apiReplyJira1234)
        server.enqueue(authFailed)
        server.enqueue(apiReplyProd42)
        server.enqueue(apiReplyProd42)

        server.start()
        val serverURL = server.url("").toString()

        try {
            setupConfigMap(true, JiraBotReplyMode.INLINE)
            configMap = configMap.plus(ConfigurationConstants.jiraRestServiceBaseUrl to serverURL)
            val jiraRestClient = Jira7RestClientImpl(configMap)

            // 1st request: will get 200
            val apiCall1 = jiraRestClient.resolve(serverURL, jiraBrowseIssueBaseUrl, "JIRA-1234")
            assertEquals("JiraEntity(key=JIRA-1234, jiraIssueBrowseURL=http://localhost/browse, summary=A subtask with some summary here, " +
                    "fieldsMap={summary=Some summary here, created=2017-03-17T15:37:10.000+0100, updated=2017-07-17T10:42:55.000+0200, status.name=Closed, " +
                    "priority.name=Minor, reporter.name=jdoe, assignee.name=noone})",
                    apiCall1.toString())

            // 2nd request: will get 401, then should auto retry, then will get 200
            val apiCall2 = jiraRestClient.resolve(serverURL, jiraBrowseIssueBaseUrl, "PROD-42")
            assertEquals("JiraEntity(key=PROD-42, jiraIssueBrowseURL=http://localhost/browse, summary=, " +
                    "fieldsMap={summary=Another summary here available in fields, created=2017-03-17T15:38:10.000+0100, updated=2017-07-17T10:43:55.000+0200, " +
                    "status.name=Closed, priority.name=Minor, reporter.name=bkelso, assignee.name=jdorian})",
                    apiCall2.toString())

            // 3rd request: will get 200
            val apiCall3 = jiraRestClient.resolve(serverURL, jiraBrowseIssueBaseUrl, "PROD-42")
            assertEquals("JiraEntity(key=PROD-42, jiraIssueBrowseURL=http://localhost/browse, summary=, " +
                    "fieldsMap={summary=Another summary here available in fields, created=2017-03-17T15:38:10.000+0100, updated=2017-07-17T10:43:55.000+0200, " +
                    "status.name=Closed, priority.name=Minor, reporter.name=bkelso, assignee.name=jdorian})",
                    apiCall3.toString())


            // request 1: cold auth
            assertRequest(server.takeRequest(), "Basic c29tZXVzZXI6c29tZXB3ZA==", null)
            // request 2, with cookie (simulated as a reject)
            assertRequest(server.takeRequest(), null, "JSESSIONID=fake1")
            // request 2 getting replayed with auth
            assertRequest(server.takeRequest(), "Basic c29tZXVzZXI6c29tZXB3ZA==", null)
            // request 3: cookie
            assertRequest(server.takeRequest(), null, "JSESSIONID=fake2")
        } finally {
            server.shutdown();
        }
    }

    fun assertRequest(request: RecordedRequest, expectedAuthorization: String?, expectedCookie: String?) {
        println("> = ${request.path} - ${request.headers}")
        assertEquals(expectedAuthorization, request.getHeader("Authorization"))
        assertEquals(expectedCookie, request.getHeader("Cookie"))

        val headers = request.headers.toMultimap()
        assertTrue("Expected no duplicate header, got: $headers", headers.all { header -> header.value.size == 1 })
    }
}
