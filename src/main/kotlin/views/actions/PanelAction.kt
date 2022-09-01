package views.actions

import com.intellij.openapi.project.Project
import javax.swing.JPanel
import editor.Diagram

abstract class PanelAction(val name: String, val project: Project, val diagram: Diagram) {
    abstract fun execute(x: Int, y: Int, panel: JPanel)
}
