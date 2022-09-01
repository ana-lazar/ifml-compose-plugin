package views.actions

import com.intellij.openapi.project.Project
import javax.swing.*
import editor.Diagram
import editor.GraphicElement
import editor.nodes.ViewContainer
import services.modifiers.EventHandlerCreator
import services.parsers.ComponentParser
import views.dialogs.AddEventHandlerDialog

class AddEventHandlerAction(name: String, project: Project, diagram: Diagram) : ViewAction(name, project, diagram) {
    private val creator = EventHandlerCreator(project)

    fun create(event: String, selected: String, viewContainer: ViewContainer) {
        if (selected == "Custom") {
            creator.create(event, true, viewContainer)
        }
        else {
            creator.create(selected, false, viewContainer)
        }
    }

    override fun execute(graphicElement: GraphicElement, panel: JPanel) {
        try {
            val viewContainer = graphicElement as ViewContainer
            val events = ComponentParser.getInstance(project)?.findEvents(viewContainer, project)
            AddEventHandlerDialog(events, this::create, viewContainer).show()
        }
        catch (_: Exception) { }
    }
}
