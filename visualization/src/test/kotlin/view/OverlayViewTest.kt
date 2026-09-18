package view

import critters.world.Position
import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayViewTest {

    @Test
    fun `position inside memory area is not tinted`() {
        val memoryArea = setOf(Position(1, 1), Position(2, 2))
        assertEquals(false, shouldTint(Position(1, 1), memoryArea))
    }

    @Test
    fun `position outside memory area is tinted`() {
        val memoryArea = setOf(Position(1, 1), Position(2, 2))
        assertEquals(true, shouldTint(Position(5, 5), memoryArea))
    }

    @Test
    fun `null memory area is never tinted`() {
        assertEquals(false, shouldTint(Position(5, 5), null))
    }
}
