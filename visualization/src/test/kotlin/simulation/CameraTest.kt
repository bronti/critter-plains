package simulation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.openrndr.math.Vector2

class CameraTest {

    private fun newCamera() = Camera(mapWidth = 50, mapHeight = 50, visibleCols = 10, visibleRows = 10, cellSize = 15)

    @Test
    fun `panning in one direction moves the viewport along that axis only`() {
        val camera = newCamera()
        camera.pan(0, -3)
        assertEquals(0, camera.viewportX)
        assertEquals(0, camera.viewportY)
    }

    @Test
    fun `panning two perpendicular directions moves both axes`() {
        val camera = newCamera()
        camera.pan(3, -3)
        assertEquals(3, camera.viewportX)
        assertEquals(0, camera.viewportY)
    }

    @Test
    fun `panning toward an edge already reached stays clamped with no overshoot`() {
        val camera = newCamera()
        camera.pan(0, -3)
        assertEquals(0, camera.viewportY)
        camera.pan(0, -3)
        assertEquals(0, camera.viewportY)
    }

    @Test
    fun `panning clamps at the far edge of the map`() {
        val camera = newCamera()
        repeat(20) { camera.pan(100, 100) }
        assertEquals(40, camera.viewportX)
        assertEquals(40, camera.viewportY)
    }

    @Test
    fun `mapPosition converts screen coordinates using the current viewport offset`() {
        val camera = newCamera()
        camera.pan(5, 5)
        val position = camera.mapPosition(Vector2(30.0, 15.0))
        assertEquals(7, position?.x)
        assertEquals(6, position?.y)
    }

    @Test
    fun `mapPosition returns null outside map bounds`() {
        val camera = Camera(mapWidth = 5, mapHeight = 5, visibleCols = 10, visibleRows = 10, cellSize = 15)
        assertNull(camera.mapPosition(Vector2(999.0, 999.0)))
    }
}
