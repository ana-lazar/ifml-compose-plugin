package views.actions

import com.intellij.openapi.project.Project
import javax.swing.JOptionPane
import javax.swing.JPanel
import editor.Diagram
import editor.GraphicElement
import editor.nodes.ViewContainer
import services.modifiers.ParameterModifier

class ModifyParameterAction(name: String, private val message: String, project: Project, diagram: Diagram) : ViewAction(name, project, diagram) {
    private val creator = ParameterModifier(project)

    override fun execute(graphicElement: GraphicElement, panel: JPanel) {
        try {
            val oldName = JOptionPane.showInputDialog(panel, message, name, JOptionPane.QUESTION_MESSAGE)
            val newName = JOptionPane.showInputDialog(panel, message, name, JOptionPane.QUESTION_MESSAGE)
            creator.modify(newName, oldName, graphicElement as ViewContainer)
        }
        catch (_: Exception) { }
    }
}
