package editor

import java.awt.Graphics
import java.io.Serializable;

abstract class GraphicElement : Serializable {
    abstract fun draw(g: Graphics)

    companion object {
        private const val serialVersionUID = -7357466511459361679L
    }
}
