package input

import org.junit.Assert.assertEquals
import org.junit.Test

class InputHandlerTest {

    @Test
    fun `w pans up`() {
        assertEquals(0 to -1, panDelta(setOf("w"), 1))
    }

    @Test
    fun `s pans down`() {
        assertEquals(0 to 1, panDelta(setOf("s"), 1))
    }

    @Test
    fun `a pans left`() {
        assertEquals(-1 to 0, panDelta(setOf("a"), 1))
    }

    @Test
    fun `d pans right`() {
        assertEquals(1 to 0, panDelta(setOf("d"), 1))
    }

    @Test
    fun `arrow-up pans up same as w`() {
        assertEquals(0 to -1, panDelta(setOf("arrow-up"), 1))
    }

    @Test
    fun `arrow-down pans down same as s`() {
        assertEquals(0 to 1, panDelta(setOf("arrow-down"), 1))
    }

    @Test
    fun `arrow-left pans left same as a`() {
        assertEquals(-1 to 0, panDelta(setOf("arrow-left"), 1))
    }

    @Test
    fun `arrow-right pans right same as d`() {
        assertEquals(1 to 0, panDelta(setOf("arrow-right"), 1))
    }

    @Test
    fun `opposing keys held together cancel out`() {
        assertEquals(0 to 0, panDelta(setOf("w", "s"), 1))
        assertEquals(0 to 0, panDelta(setOf("a", "d"), 1))
    }

    @Test
    fun `perpendicular keys held together combine diagonally`() {
        assertEquals(1 to -1, panDelta(setOf("w", "d"), 1))
    }

    @Test
    fun `no keys held produces zero delta`() {
        assertEquals(0 to 0, panDelta(emptySet(), 1))
    }

    @Test
    fun `speed scales the delta`() {
        assertEquals(0 to -5, panDelta(setOf("w"), 5))
    }

    @Test
    fun `equals key zooms in`() {
        assertEquals(1.25, zoomFactorForKey("="))
    }

    @Test
    fun `keypad plus key zooms in same as equals`() {
        assertEquals(1.25, zoomFactorForKey("+"))
    }

    @Test
    fun `minus key zooms out`() {
        assertEquals(1.0 / 1.25, zoomFactorForKey("-"))
    }

    @Test
    fun `a non-zoom key returns no zoom factor`() {
        assertEquals(null, zoomFactorForKey("w"))
    }

    @Test
    fun `scrolling up zooms in`() {
        assertEquals(1.1, zoomFactorForScroll(1.0))
    }

    @Test
    fun `scrolling down zooms out`() {
        assertEquals(1.0 / 1.1, zoomFactorForScroll(-1.0))
    }

    @Test
    fun `zero vertical scroll produces no zoom factor`() {
        assertEquals(null, zoomFactorForScroll(0.0))
    }

    @Test
    fun `cursor inside the map viewport is over the map`() {
        assertEquals(true, isOverMapViewport(x = 100.0, viewportWidthPx = 200))
    }

    @Test
    fun `cursor over the info panel is not over the map`() {
        assertEquals(false, isOverMapViewport(x = 250.0, viewportWidthPx = 200))
    }

    @Test
    fun `cursor exactly at the viewport boundary is not over the map`() {
        assertEquals(false, isOverMapViewport(x = 200.0, viewportWidthPx = 200))
    }
}
