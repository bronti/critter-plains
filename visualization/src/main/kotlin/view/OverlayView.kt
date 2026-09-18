package view

import critters.CritterStateView
import critters.world.Position
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import kotlin.math.floor

// Semi-transparent dark fill: reads as "fog" over the terrain colors underneath without fully
// hiding them. OPENRNDR's default BlendMode.OVER does straight (non-premultiplied) alpha
// blending, so this alpha value composites against whatever BufferedWorldView already blitted.
private val FOG_TINT = ColorRGBa(0.0, 0.0, 0.0, 0.5)

// Pure membership check, kept top-level and nullable-first so "nothing selected" is just one
// more input rather than a separate code path, mirroring InputHandler.kt's extraction pattern.
internal fun shouldTint(pos: Position, memoryArea: Set<Position>?): Boolean =
    memoryArea != null && pos !in memoryArea

class OverlayView(private val drawer: Drawer) {

    fun render(
        selectedCritter: CritterStateView?,
        cellSize: Double,
        viewportX: Double,
        viewportY: Double,
        viewportWidthPx: Int,
        viewportHeightPx: Int
    ) {
        if (selectedCritter == null) return

        drawer.fill = FOG_TINT
        drawer.stroke = null
        drawer.strokeWeight = 0.0

        val memoryArea = selectedCritter.memory.area()

        val firstCol = floor(viewportX).toInt()
        val firstRow = floor(viewportY).toInt()
        val fracX = viewportX - firstCol
        val fracY = viewportY - firstRow

        val colCount = (viewportWidthPx / cellSize).toInt() + 1
        val rowCount = (viewportHeightPx / cellSize).toInt() + 1

        for (col in 0 until colCount) {
            for (row in 0 until rowCount) {
                val pos = Position(firstCol + col, firstRow + row)
                if (!shouldTint(pos, memoryArea)) continue
                val screenX = (col - fracX) * cellSize
                val screenY = (row - fracY) * cellSize
                drawer.rectangle(screenX, screenY, cellSize, cellSize)
            }
        }
    }
}
