package view

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.math.Vector2
import simulation.World
import kotlin.math.floor

class BufferedWorldView(
    private val drawer: Drawer,
    private val world: World,
    private val buffer: RenderTarget
) {
    fun render(cellSize: Double, viewportX: Double, viewportY: Double) = drawer.isolatedWithTarget(buffer) {
        clear(ColorRGBa.BLACK)
        stroke = null

        val firstCol = floor(viewportX).toInt()
        val firstRow = floor(viewportY).toInt()
        val fracX = viewportX - firstCol
        val fracY = viewportY - firstRow

        // +1 covers the tile partially revealed at the trailing edge by the fractional
        // viewport offset, so only tiles actually visible in the buffer are drawn.
        val colCount = (buffer.width / cellSize).toInt() + 1
        val rowCount = (buffer.height / cellSize).toInt() + 1

        for (col in 0 until colCount) {
            for (row in 0 until rowCount) {
                val screenX = (col - fracX) * cellSize
                val screenY = (row - fracY) * cellSize
                // Buffer-local, un-offset pixel position: World.mapPosition() re-applies the
                // live viewportX/viewportY internally, so this resolves to the correct
                // absolute world tile (firstCol + col, firstRow + row).
                val worldLookupPos = Vector2(col * cellSize, row * cellSize)
                fill = ColorRGBa.fromHex(world.territory(worldLookupPos).hexColor)
                rectangle(screenX, screenY, cellSize, cellSize)
                if (world.occupied(worldLookupPos)) {
                    fill = ColorRGBa.BLACK
                    circle(screenX + cellSize / 2.0, screenY + cellSize / 2.0, cellSize / 2.0)
                }
            }
        }
    }
}
