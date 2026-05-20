import critters.Game
import critters.world.Position
import critters.world.World
import org.openrndr.KEY_SPACEBAR
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.loadFont
import org.openrndr.draw.renderTarget

fun runVisualization(game: Game, cellSize: Int, fps: Int) = application {
    configure {
        width = game.mapWidth * cellSize
        height = game.mapHeight * cellSize
        title = "Terrain Map ${width}x$height"
    }

    program {
        val updateInterval = 1.0 / fps
        var lastUpdateTime = -updateInterval
        var paused = false

        keyboard.keyDown.listen {
            if (it.key == KEY_SPACEBAR) paused = !paused
        }

        val mapBuffer = renderTarget(width, height) {
            colorBuffer()
        }

        fun updateMap(world: Game.State) {
            drawer.isolatedWithTarget(mapBuffer) {
                clear(ColorRGBa.BLACK)
                stroke = null

                for (x in 0 until game.mapWidth) {
                    for (y in 0 until game.mapHeight) {
                        val pos = Position(x, y)
                        fill = ColorRGBa.fromHex(world.territory(pos).hexColor)
                        rectangle(
                            cellSize * x.toDouble(),
                            cellSize * y.toDouble(),
                            cellSize.toDouble(),
                            cellSize.toDouble()
                        )
                        val occupant = world.occupant(pos)
                        if (occupant != null) {
                            fill = ColorRGBa.BLACK
                            val halfCellSize = cellSize / 2
                            circle(
                                (cellSize * x + halfCellSize).toDouble(),
                                (cellSize * y + halfCellSize).toDouble(),
                                halfCellSize.toDouble()
                            )
                        }
                    }
                }
            }
        }

        extend {
            if (!paused && seconds - lastUpdateTime >= updateInterval) {
                game.tick()
                updateMap(game.state())
                lastUpdateTime = seconds
            }

            drawer.image(mapBuffer.colorBuffer(0), 0.0, 0.0, width.toDouble(), height.toDouble())

            if (paused) {
                val world = game.state()
                val mousePos = mouse.position
                val cellX = (mousePos.x / cellSize).toInt()
                val cellY = (mousePos.y / cellSize).toInt()
                if (cellX in 0 until game.mapWidth && cellY in 0 until game.mapHeight) {
                    val pos = Position(cellX, cellY)
                    val occupant = world.occupant(pos)
                    if (occupant != null) {
                        val label = "${occupant.name}, hunger: ${occupant.hunger}"
                        val textX = mousePos.x + 10.0
                        val textY = mousePos.y - 10.0
                        drawer.fill = ColorRGBa.BLACK.opacify(0.6)
                        drawer.stroke = null

                        val font = loadFont("data/fonts/default.otf", 50.0)
                        drawer.fontMap = font

//                        val bounds = drawer.fontImage.characterBounds(label)
//                        val padding = 4.0
//                        drawer.rectangle(
//                            textX - padding,
//                            textY - bounds.height - padding,
//                            bounds.width + padding * 2,
//                            bounds.height + padding * 2
//                        )
                        drawer.fill = ColorRGBa.WHITE
                        drawer.text(label, textX, textY)
                    }
                }
            }
        }
    }
}
