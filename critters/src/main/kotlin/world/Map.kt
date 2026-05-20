package critters.world

import kotlin.math.sqrt

sealed class Territory(val hexColor: String, val traversable: Boolean)

data object VOID : Territory("#000000", false) // Singletons use 'data object'

sealed class Terrain(hexColor: String) : Territory(hexColor, true) {
    data object SOIL : Terrain("#6A381F")

//    data object WATER : Terrain("#202C59")
//    data object SAND : Terrain("#F2A541")
//    data object MOUNTAIN : Terrain("#76818E")
    data object FOOD : Terrain("#514B23");

    companion object {
        val ground by lazy {
            SOIL
        }
    }
}

// todo: do we want to ensure map size compliance here?
data class Position(val x: Int, val y: Int) {
    fun distance(other: Position): Double =
        sqrt(((x - other.x) * (x - other.x) + (y - other.y) * (y - other.y)).toDouble())
}

interface GameMap {
    val width: Int
    val height: Int

    // contract: for positions outer bounds returns VOID
    fun territory(pos: Position): Territory

    companion object
}

fun GameMap.traversable(pos: Position): Boolean = territory(pos) != VOID

// todo: use persistent maps
class MutableGameMap internal constructor(
    private val grid: Array<Array<Terrain>>,
    override val width: Int,
    override val height: Int
) : GameMap {
    init {
        require(width > 10) { "Map width $width is supposed to be greater than 10" }
        require(height > 10) { "Map height $height is supposed to be greater than 10" }
        require(grid.size == width) { "The grids width is ${grid.size} instead of $width" }
        grid.forEachIndexed { index, row ->
            require(row.size == height) { "The length of line number $index is ${row.size} instead of $height" }
        }
    }

    constructor(map: MutableGameMap) : this(map.grid.clone(), map.width, map.height)

    override fun territory(pos: Position): Territory = if (inBounds(pos)) grid[pos.x][pos.y] else VOID

    fun replace(pos: Position, terrain: Terrain): MutableGameMap {
        grid[pos.x][pos.y] = terrain
        return this
    }

    private fun inBounds(pos: Position) = pos.x in 0 until width && pos.y in 0 until height
}
