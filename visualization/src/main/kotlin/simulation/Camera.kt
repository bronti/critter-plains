package simulation

import critters.world.Position
import org.openrndr.math.Vector2

class Camera(
    private val mapWidth: Int,
    private val mapHeight: Int,
    private val visibleCols: Int,
    private val visibleRows: Int,
    private val cellSize: Int,
) {

    var viewportX: Int = 0
        private set
    var viewportY: Int = 0
        private set

    fun pan(dx: Int, dy: Int) {
        viewportX = (viewportX + dx).coerceIn(0, maxOf(0, mapWidth - visibleCols))
        viewportY = (viewportY + dy).coerceIn(0, maxOf(0, mapHeight - visibleRows))
    }

    fun mapPosition(pos: Vector2): Position? {
        val cellX = (pos.x / cellSize).toInt() + viewportX
        val cellY = (pos.y / cellSize).toInt() + viewportY
        return if (cellX in 0 until mapWidth && cellY in 0 until mapHeight)
            Position(cellX, cellY)
        else null
    }
}
