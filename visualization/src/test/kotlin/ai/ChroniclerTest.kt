package ai

import org.junit.Assert.assertEquals
import org.junit.Test

class ChroniclerTest {

    @Test
    fun `forceDisabled keeps the chronicler disabled regardless of environment`() {
        val chronicler = Chronicler(forceDisabled = true)
        assertEquals("(set ANTHROPIC_API_KEY to enable the AI chronicler)", chronicler.latestChronicle)
    }
}
