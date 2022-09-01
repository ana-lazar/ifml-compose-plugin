package views.actions

import com.intellij.openapi.project.Project
import javax.swing.*
import editor.Diagram
import editor.GraphicElement
import editor.nodes.ViewContainer
import utils.Parameter
import utils.ComponentUtils
import services.modifiers.ParameterCreator
import views.dialogs.AddParameterDialog

class AddParameterAction(name: String, project: Project, diagram: Diagram) : ViewAction(name, project, diagram) {
    private val creator = ParameterCreator(project)

    fun create(name: String, value: String, type: String, isState: Boolean, viewContainer: ViewContainer) {
        creator.create(Parameter(name, type, value, isState), viewContainer)
    }

    override fun execute(graphicElement: GraphicElement, panel: JPanel) {
        try {
            val viewContainer = graphicElement as ViewContainer
            AddParameterDialog(ComponentUtils.parameterTypes, viewContainer, this::create).show()
        }
        catch (_: Exception) { }
    }
}
