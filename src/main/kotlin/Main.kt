import critters.Game

const val gridSize = 150
const val cellPixelSize = 15
const val fps = 5

const val viewportWidth = 900
const val viewportHeight = 750

fun main() {
    val game = Game(gridSize, gridSize, 50)
    runVisualization(game, cellPixelSize, fps, viewportWidth, viewportHeight)
}
