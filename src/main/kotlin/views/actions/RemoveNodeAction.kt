package views.actions

import com.intellij.openapi.project.Project
import javax.swing.JPanel
import editor.Diagram
import editor.GraphicElement
import editor.nodes.Action
import editor.nodes.ViewContainer
import services.modifiers.ActionRemover
import services.modifiers.ViewRemover

class RemoveNodeAction(name: String, project: Project, diagram: Diagram) : ViewAction(name, project, diagram) {
    private val activityRemover = ViewRemover(project, diagram)
    private val actionRemover = ActionRemover(project)

    override fun execute(graphicElement: GraphicElement, panel: JPanel) {
        try {
            if (graphicElement is Action) {
                actionRemover.remove(graphicElement)
            }
            else if (graphicElement is ViewContainer) {
                activityRemover.remove(graphicElement)
            }
        }
        catch (_: Exception) { }
    }
}
