import critters.Game

const val gridSize = 50
const val cellPixelSize = 15
const val fps = 5

fun main() {
    val game = Game(gridSize, gridSize, 20)
    runVisualization(game, cellPixelSize, fps)
}
