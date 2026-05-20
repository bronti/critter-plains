package critters.world

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

class GameMapBuilder(val width: Int, val height: Int, private val grid: Array<Array<Terrain>>) {
    init {
        require(width > 10) { "Map width $width is supposed to be greater than 10" }
        require(height > 10) { "Map height $height is supposed to be greater than 10" }
    }

    constructor(width: Int, height: Int) : this(width, height, Terrain.ground)
    constructor(width: Int, height: Int, defaultTerrain: Terrain) : this(
        width,
        height,
        Array(width) { Array(height) { defaultTerrain } })

    fun build(): MutableGameMap = MutableGameMap(grid.clone(), width, height)

    fun addBlob(terrain: Terrain): GameMapBuilder {
        val center = (0 until width).random() to (0 until height).random()
        val minRadius = max(1, (width + height) / 20)
        val maxRadius = max(4, (width + height) / 10)
        val radius = (minRadius..maxRadius).random()
        for (y in center.second - radius..center.second + radius) {
            if (y < 0) continue
            if (y >= height) break
            val yShift = abs(center.second - y)
            val xRadius = floor(sqrt((radius * radius - yShift * yShift).toDouble())).toInt()
            for (x in center.first - xRadius..center.first + xRadius) {
                if (x < 0) continue
                if (x >= width) break
                grid[x][y] = terrain
            }
        }
        return this
    }
}
