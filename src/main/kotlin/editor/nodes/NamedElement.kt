package editor.nodes

import java.awt.Graphics
import editor.GraphicElement

open class NamedElement(var text: String) : GraphicElement() {
    var x: Int = 0
    var y: Int = 0

    fun position(setX: Int, setY: Int) {
        x = setX
        y = setY
    }

    override fun draw(g: Graphics) {
        g.drawString(text, x, y)
    }
}

open class ParameterElement(var name: String, type: String) : NamedElement("$name = $type")

class StateParameterElement(var originalName: String, type: String) : ParameterElement("<state> $originalName", type)
