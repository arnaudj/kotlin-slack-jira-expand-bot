import com.github.arnaudj.Hello
import org.junit.Assert
import org.junit.Test

class HelloTest {
    @Test fun `basic smoke test`() {
        Assert.assertEquals("my name", Hello("my name").name)
    }
}