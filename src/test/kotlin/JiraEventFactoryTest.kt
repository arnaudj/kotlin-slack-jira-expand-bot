import com.github.arnaudj.linkify.engines.jira.JiraEventFactory
import com.github.arnaudj.linkify.engines.jira.entities.JiraEntity
import com.github.arnaudj.linkify.engines.jira.entities.JiraSeenEvent
import com.github.arnaudj.linkify.eventdriven.events.Event
import com.github.arnaudj.linkify.eventdriven.events.EventSourceData
import org.junit.Test

class JiraEventFactoryTest {
    val dummySourceData = EventSourceData("src", "uid", "ts", "tid")

    @Test
    fun `test no reference found`() {
        testEventExtraction("The quick brown fox jumps over the lazy dog")
        testEventExtraction("http://aaaaaaaaa.bbbbbbbbbb/some-seo-friendly-link-2-with-trailer/")
        testEventExtraction("http://aaaaaaaaa.bbbbbbbbbb/SOME-SEO-FRIENDLY-LINK-2-WITH-TRAILER/")
        testEventExtraction("Charsets: UTF-8 CP-1252 ISO-8859-1")
    }

    @Test
    fun `test 1 reference found`() {
        val seenJiraProd1 = JiraSeenEvent(dummySourceData, JiraEntity("PROD-1"))
        testEventExtraction("Message with some issue PROD-1", seenJiraProd1)
        testEventExtraction("Message with some issue PROD-1 embedded", seenJiraProd1)
        testEventExtraction("PROD-1", seenJiraProd1)
        testEventExtraction("PROD-1!", seenJiraProd1)
        testEventExtraction("PROD-1 and some text", seenJiraProd1)
    }

    @Test
    fun `test 2 references found`() {
        val seenJiraProd1 = JiraSeenEvent(dummySourceData, JiraEntity("PROD-1"))
        val seenJiraProd2 = JiraSeenEvent(dummySourceData, JiraEntity("PROD-200"))
        testEventExtraction("Message with some issues PROD-1 and PROD-200", seenJiraProd1, seenJiraProd2)
        testEventExtraction("Message with some issues PROD-1 and PROD-200 embedded", seenJiraProd1, seenJiraProd2)
        testEventExtraction("PROD-1 PROD-200", seenJiraProd1, seenJiraProd2)
        testEventExtraction("PROD-1 PROD-200!", seenJiraProd1, seenJiraProd2)
        testEventExtraction("PROD-1,PROD-200 and some text", seenJiraProd1, seenJiraProd2)
        testEventExtraction("PROD-1&PROD-200 and some text", seenJiraProd1, seenJiraProd2)
        testEventExtraction("PROD-1/PROD-200 and some text", seenJiraProd1, seenJiraProd2)
    }

    private fun testEventExtraction(actualText: String, vararg expectedEvents: Event) {
        val events = JiraEventFactory().createFrom(actualText, dummySourceData)
        assert(expectedEvents.contentEquals(events.toTypedArray())) { "Expected:\n" + expectedEvents.contentToString() + "\n\n" + "Actual:\n" + events.toString() }
    }
}