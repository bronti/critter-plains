package view

import critters.GameStateView
import critters.world.Position
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.isolatedWithTarget
import kotlin.math.floor

class BufferedWorldView(
    private val drawer: Drawer,
    private val buffer: RenderTarget
) {
    fun render(state: GameStateView, cellSize: Double, viewportX: Double, viewportY: Double) =
        drawer.isolatedWithTarget(buffer) {
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
                    // Exact integer world position from the loop indices directly — no
                    // multiply-by-cellSize-then-divide-by-cellSize round-trip, which lost
                    // precision at some zoom levels and caused duplicated/skipped tile columns.
                    val pos = Position(firstCol + col, firstRow + row)
                    fill = ColorRGBa.fromHex(state.territory(pos).hexColor)
                    rectangle(screenX, screenY, cellSize, cellSize)
                    if (state.occupant(pos) != null) {
                        fill = ColorRGBa.BLACK
                        circle(screenX + cellSize / 2.0, screenY + cellSize / 2.0, cellSize / 2.0)
                    }
                }
            }
        }
}
