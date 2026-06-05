package view

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.math.Vector2
import simulation.WorldData

class BufferedWorldView(
    private val drawer: Drawer,
    private val mapWidth: Int,
    private val mapHeight: Int,
    private val cellSize: Int,
    private val data: WorldData,
    private val buffer: RenderTarget
) {
    fun render() = drawer.isolatedWithTarget(buffer) {
        clear(ColorRGBa.BLACK)
        stroke = null

        val cellSizeD = cellSize.toDouble()

        for (x in 0 until mapWidth) {
            for (y in 0 until mapHeight) {
                val pos = Vector2(x * cellSizeD, y * cellSizeD)
                fill = ColorRGBa.fromHex(data.territory(pos).hexColor)
                rectangle(pos.x, pos.y, cellSizeD, cellSizeD)
                if (data.occupied(pos)) {
                    fill = ColorRGBa.BLACK
                    circle(pos.x + cellSize / 2.0, pos.y + cellSize / 2.0, cellSize / 2.0)
                }
            }
        }
    }
}
