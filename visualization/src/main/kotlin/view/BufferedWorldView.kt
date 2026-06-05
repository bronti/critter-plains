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

        for (x in 0 until world.mapWidth) {
            for (y in 0 until world.mapHeight) {
                val pos = Vector2(x * cellSizeD, y * cellSizeD)
                fill = ColorRGBa.fromHex(world.territory(pos).hexColor)
                rectangle(pos.x, pos.y, cellSizeD, cellSizeD)
                if (world.occupied(pos)) {
                    fill = ColorRGBa.BLACK
                    circle(pos.x + cellSize / 2.0, pos.y + cellSize / 2.0, cellSize / 2.0)
                }
            }
        }
    }
}
