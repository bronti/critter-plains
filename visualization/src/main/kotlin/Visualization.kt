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
import view.OverlayView

private const val INFO_PANEL_WIDTH = 200.0

fun runVisualization(game: Game, cellSize: Int, fps: Int, viewportWidth: Int, viewportHeight: Int) = application {
    configure {
        width = viewportWidth + INFO_PANEL_WIDTH.toInt()
        height = viewportHeight
        title = "Critter Plains"
    }

    program {
        val font = loadFont("data/fonts/default.otf", 20.0)

        val controller = SimulationController(game, fps, seconds, cellSize, viewportWidth, viewportHeight)

        val buffer: RenderTarget = renderTarget(viewportWidth, viewportHeight) {
            colorBuffer()
        }
        val worldView = BufferedWorldView(drawer, buffer)
        val overlayView = OverlayView(drawer)
        val hudView = HudView(drawer, font)
        val infoPanel = InfoPanel(drawer, font, width - INFO_PANEL_WIDTH, INFO_PANEL_WIDTH, height.toDouble())
        val inputHandler = InputHandler(this, controller)

        inputHandler.setup()

        extend {
            inputHandler.pollHeldKeys()

            controller.update(seconds)

            worldView.render(state = controller.state, cellSize = controller.cellSize, viewportX = controller.viewportX, viewportY = controller.viewportY)

            drawer.image(buffer.colorBuffer(0), 0.0, 0.0, width.toDouble(), height.toDouble())

            overlayView.render(
                selectedCritter = controller.selectedCritter,
                cellSize = controller.cellSize,
                viewportX = controller.viewportX,
                viewportY = controller.viewportY,
                viewportWidthPx = controller.viewportWidthPx,
                viewportHeightPx = controller.viewportHeightPx
            )

            val mousePos = mouse.position
            val hoveredCritter = controller.world.occupant(mousePos)
            if (hoveredCritter != null) {
                hudView.render(hoveredCritter, mousePos)
            }

            infoPanel.render(controller.selectedCritter, controller.chronicle)
        }
    }
}
