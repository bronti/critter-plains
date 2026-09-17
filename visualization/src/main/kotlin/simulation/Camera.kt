package simulation

import critters.world.Position
import org.openrndr.math.Vector2
import kotlin.math.max

class Camera(
    private val mapWidth: Int,
    private val mapHeight: Int,
    private val viewportWidthPx: Int,
    private val viewportHeightPx: Int,
    initialCellSize: Double,
) {

    companion object {
        const val MIN_CELL_SIZE = 5.0
        const val MAX_CELL_SIZE = 45.0
    }

    var cellSize: Double = initialCellSize.coerceIn(MIN_CELL_SIZE, MAX_CELL_SIZE)
        private set
    var viewportX: Double = 0.0
        private set
    var viewportY: Double = 0.0
        private set

    val visibleCols: Int get() = (viewportWidthPx / cellSize).toInt()
    val visibleRows: Int get() = (viewportHeightPx / cellSize).toInt()

    fun pan(dx: Int, dy: Int) {
        viewportX = (viewportX + dx).coerceIn(0.0, maxViewportX())
        viewportY = (viewportY + dy).coerceIn(0.0, maxViewportY())
    }

    fun zoomAt(anchorScreenPos: Vector2, factor: Double) {
        val worldAnchorX = viewportX + anchorScreenPos.x / cellSize
        val worldAnchorY = viewportY + anchorScreenPos.y / cellSize

        cellSize = (cellSize * factor).coerceIn(MIN_CELL_SIZE, MAX_CELL_SIZE)

        viewportX = (worldAnchorX - anchorScreenPos.x / cellSize).coerceIn(0.0, maxViewportX())
        viewportY = (worldAnchorY - anchorScreenPos.y / cellSize).coerceIn(0.0, maxViewportY())
    }

    fun mapPosition(pos: Vector2): Position? {
        val cellX = (pos.x / cellSize + viewportX).toInt()
        val cellY = (pos.y / cellSize + viewportY).toInt()
        return if (cellX in 0 until mapWidth && cellY in 0 until mapHeight)
            Position(cellX, cellY)
        else null
    }

    private fun maxViewportX() = max(0.0, mapWidth - viewportWidthPx / cellSize)
    private fun maxViewportY() = max(0.0, mapHeight - viewportHeightPx / cellSize)
}
