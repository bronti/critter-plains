package simulation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openrndr.math.Vector2

private const val EPSILON = 1e-9

class CameraTest {

    private fun newCamera(initialCellSize: Double = 15.0) =
        Camera(mapWidth = 50, mapHeight = 50, viewportWidthPx = 150, viewportHeightPx = 150, initialCellSize = initialCellSize)

    @Test
    fun `panning in one direction moves the viewport along that axis only`() {
        val camera = newCamera()
        camera.pan(0, -3)
        assertEquals(0.0, camera.viewportX, EPSILON)
        assertEquals(0.0, camera.viewportY, EPSILON)
    }

    @Test
    fun `panning two perpendicular directions moves both axes`() {
        val camera = newCamera()
        camera.pan(3, -3)
        assertEquals(3.0, camera.viewportX, EPSILON)
        assertEquals(0.0, camera.viewportY, EPSILON)
    }

    @Test
    fun `panning toward an edge already reached stays clamped with no overshoot`() {
        val camera = newCamera()
        camera.pan(0, -3)
        assertEquals(0.0, camera.viewportY, EPSILON)
        camera.pan(0, -3)
        assertEquals(0.0, camera.viewportY, EPSILON)
    }

    @Test
    fun `panning clamps at the far edge of the map`() {
        val camera = newCamera()
        repeat(20) { camera.pan(100, 100) }
        assertEquals(40.0, camera.viewportX, EPSILON)
        assertEquals(40.0, camera.viewportY, EPSILON)
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
        val camera = Camera(mapWidth = 5, mapHeight = 5, viewportWidthPx = 150, viewportHeightPx = 150, initialCellSize = 15.0)
        assertNull(camera.mapPosition(Vector2(999.0, 999.0)))
    }

    @Test
    fun `zooming in increases cellSize and keeps the world point under the anchor fixed`() {
        val camera = newCamera()
        val anchor = Vector2(75.0, 75.0)
        val worldAnchorBefore = camera.viewportX + anchor.x / camera.cellSize

        camera.zoomAt(anchor, 2.0)

        assertEquals(30.0, camera.cellSize, EPSILON)
        val worldAnchorAfter = camera.viewportX + anchor.x / camera.cellSize
        assertEquals(worldAnchorBefore, worldAnchorAfter, EPSILON)
    }

    @Test
    fun `zooming out decreases cellSize and keeps the world point under the anchor fixed`() {
        val camera = newCamera()
        // Pan away from the origin first so the post-zoom viewport (which needs to reveal
        // more world per pixel as cellSize shrinks) has room to move without clamping —
        // isolating the anchor-preserving formula from the clamp behavior covered separately.
        camera.pan(20, 20)
        val anchor = Vector2(75.0, 75.0)
        val worldAnchorBefore = camera.viewportX + anchor.x / camera.cellSize

        camera.zoomAt(anchor, 0.5)

        assertEquals(7.5, camera.cellSize, EPSILON)
        val worldAnchorAfter = camera.viewportX + anchor.x / camera.cellSize
        assertEquals(worldAnchorBefore, worldAnchorAfter, EPSILON)
    }

    @Test
    fun `zooming in repeatedly clamps at the max cell size with no overshoot`() {
        val camera = newCamera()
        repeat(20) { camera.zoomAt(Vector2(75.0, 75.0), 10.0) }
        assertEquals(Camera.MAX_CELL_SIZE, camera.cellSize, EPSILON)
    }

    @Test
    fun `zooming out repeatedly clamps at the min cell size with no overshoot`() {
        val camera = newCamera()
        repeat(20) { camera.zoomAt(Vector2(75.0, 75.0), 0.1) }
        assertEquals(Camera.MIN_CELL_SIZE, camera.cellSize, EPSILON)
    }

    @Test
    fun `zooming out past where the viewport exceeds the map clamps pan to show the whole map`() {
        val camera = Camera(mapWidth = 5, mapHeight = 5, viewportWidthPx = 150, viewportHeightPx = 150, initialCellSize = 15.0)
        repeat(20) { camera.zoomAt(Vector2(75.0, 75.0), 0.1) }
        assertEquals(0.0, camera.viewportX, EPSILON)
        assertEquals(0.0, camera.viewportY, EPSILON)
    }

    @Test
    fun `zooming out while panned near the far edge re-clamps the viewport to the new bound`() {
        val camera = newCamera()
        repeat(50) { camera.pan(100, 100) }
        assertEquals(40.0, camera.viewportX, EPSILON)

        camera.zoomAt(Vector2(0.0, 0.0), 0.5)

        assertEquals(7.5, camera.cellSize, EPSILON)
        // maxViewportX at cellSize 7.5 is 50 - 150/7.5 = 30, below the pre-zoom 40 — the
        // anchor-preserving formula alone would keep 40, so this also proves clamping wins.
        assertEquals(30.0, camera.viewportX, EPSILON)
        assertEquals(30.0, camera.viewportY, EPSILON)
    }

    @Test
    fun `zooming out on a non-square viewport clamps X and Y independently`() {
        // viewportWidthPx (300) and viewportHeightPx (120) are distinct, so maxViewportX and
        // maxViewportY diverge at the same cellSize — a square 150x150 viewport can't tell
        // this apart from a shared clamp bug.
        val camera = Camera(mapWidth = 50, mapHeight = 50, viewportWidthPx = 300, viewportHeightPx = 120, initialCellSize = 15.0)
        repeat(20) { camera.zoomAt(Vector2(0.0, 0.0), 0.1) }

        assertEquals(Camera.MIN_CELL_SIZE, camera.cellSize, EPSILON)
        // maxViewportX = 50 - 300/5 = -10 -> clamps to 0; maxViewportY = 50 - 120/5 = 26.
        assertEquals(0.0, camera.viewportX, EPSILON)
        assertEquals(0.0, camera.viewportY, EPSILON)

        // Pan both axes to their true max to show X and Y aren't tied to the same bound.
        camera.pan(100, 100)
        assertEquals(0.0, camera.viewportX, EPSILON)
        assertEquals(26.0, camera.viewportY, EPSILON)
    }

    @Test
    fun `mapPosition resolves correctly with a fractional viewport offset produced by zoom`() {
        val camera = newCamera()
        camera.pan(10, 10)
        // Anchor off-center so the zoom leaves a non-whole-number viewport offset.
        camera.zoomAt(Vector2(20.0, 20.0), 1.5)

        // cellSize' = 22.5; worldAnchor = 10 + 20/15 = 11.333...; viewportX' = 11.333... - 20/22.5 = 10.444...
        val expectedCellSize = 22.5
        val expectedViewportX = 10.0 + 20.0 / 15.0 - 20.0 / expectedCellSize
        assertEquals(expectedCellSize, camera.cellSize, EPSILON)
        assertEquals(expectedViewportX, camera.viewportX, EPSILON)
        assertEquals(expectedViewportX, camera.viewportY, EPSILON)
        assertTrue("expected a fractional viewport offset, got ${camera.viewportX}", camera.viewportX != kotlin.math.floor(camera.viewportX))

        // Screen pixel (45.0, 45.0) is exactly 2 cells (2 * 22.5) into the buffer, so it should
        // resolve to world tile (floor(viewportX) + 2, floor(viewportY) + 2).
        val position = camera.mapPosition(Vector2(45.0, 45.0))
        val expectedTile = kotlin.math.floor(expectedViewportX).toInt() + 2
        assertEquals(expectedTile, position?.x)
        assertEquals(expectedTile, position?.y)
    }
}
