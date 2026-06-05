import critters.Game
import input.InputHandler
import org.openrndr.application
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.loadFont
import org.openrndr.draw.renderTarget
import simulation.SimulationController
import simulation.world
import view.BufferedWorldView
import view.HudView
import view.InfoPanel

private const val INFO_PANEL_WIDTH = 200.0

fun runVisualization(game: Game, cellSize: Int, fps: Int) = application {
    configure {
        width = game.mapWidth * cellSize + INFO_PANEL_WIDTH.toInt()
        height = game.mapHeight * cellSize
        title = "Critter Plains"
    }

    program {
        val font = loadFont("data/fonts/default.otf", 20.0)

        val controller = SimulationController(game, fps, seconds, cellSize)

        val buffer: RenderTarget = renderTarget(width, height) {
            colorBuffer()
        }
        val worldView = BufferedWorldView(drawer, controller.world, cellSize, buffer)
        val hudView = HudView(drawer, font)
        val infoPanel = InfoPanel(drawer, font, width - INFO_PANEL_WIDTH, INFO_PANEL_WIDTH, height.toDouble())
        val inputHandler = InputHandler(this, controller)

        inputHandler.setup()

        extend {
            controller.update(seconds)

            worldView.render()

            drawer.image(buffer.colorBuffer(0), 0.0, 0.0, width.toDouble(), height.toDouble())

            val mousePos = mouse.position
            val hoveredCritter = controller.world.occupant(mousePos)
            if (hoveredCritter != null) {
                hudView.render(hoveredCritter, mousePos)
            }

            infoPanel.render(controller.selectedCritter)
        }
    }
}
