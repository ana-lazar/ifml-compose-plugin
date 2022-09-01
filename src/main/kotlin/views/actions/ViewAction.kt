package views.actions

import com.intellij.openapi.project.Project
import javax.swing.JPanel
import editor.Diagram
import editor.GraphicElement

abstract class ViewAction(val name: String, val project: Project, val diagram: Diagram) {
    abstract fun execute(graphicElement: GraphicElement, panel: JPanel)
}
