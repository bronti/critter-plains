package view

import critters.CritterStateView
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.FontImageMap
import org.openrndr.math.Vector2

class HudView(private val drawer: Drawer, private val font: FontImageMap) {

    fun render(critter: CritterStateView, mousePosition: Vector2) {
        val label = formatLabel(critter)
        val textX = mousePosition.x + 10.0
        val textY = mousePosition.y - 10.0

        drawer.fontMap = font
        drawer.fill = ColorRGBa.WHITE
        drawer.stroke = null
        drawer.text(label, textX, textY)
    }

    private fun formatLabel(critter: CritterStateView): String {
        // todo: expand as Critter gains more characteristics
        return "${critter.name}, hunger: ${critter.hunger}"
    }
}
