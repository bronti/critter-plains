package view

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.math.Vector2
import simulation.World

class BufferedWorldView(
    private val drawer: Drawer,
    private val world: World,
    private val cellSize: Int,
    private val buffer: RenderTarget
) {
    fun render() = drawer.isolatedWithTarget(buffer) {
        clear(ColorRGBa.BLACK)
        stroke = null

        val cellSizeD = cellSize.toDouble()

        for (col in 0 until world.width) {
            for (row in 0 until world.height) {
                val screenX = col * cellSizeD
                val screenY = row * cellSizeD
                val screenPos = Vector2(screenX, screenY)
                fill = ColorRGBa.fromHex(world.territory(screenPos).hexColor)
                rectangle(screenX, screenY, cellSizeD, cellSizeD)
                if (world.occupied(screenPos)) {
                    fill = ColorRGBa.BLACK
                    circle(screenX + cellSize / 2.0, screenY + cellSize / 2.0, cellSize / 2.0)
                }
            }
        }
    }
}
