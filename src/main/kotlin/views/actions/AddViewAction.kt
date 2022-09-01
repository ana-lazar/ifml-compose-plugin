package views.actions

import com.intellij.openapi.project.Project
import javax.swing.*
import editor.Diagram
import editor.GraphicElement
import editor.nodes.ViewContainer
import utils.Composable
import utils.ComponentUtils
import services.modifiers.ViewCreator
import services.parsers.ActivityParser
import views.dialogs.AddViewDialog

class AddViewAction(name: String, project: Project, diagram: Diagram) : ViewAction(name, project, diagram) {
    private val creator = ViewCreator(project, diagram)

    fun create(name: String, type: Composable, viewContainer: ViewContainer) {
        creator.create(name, type, viewContainer)
    }

    override fun execute(graphicElement: GraphicElement, panel: JPanel) {
        try {
            val viewContainer = graphicElement as ViewContainer
            val composables = mutableListOf(Composable("Custom", "", false, ""))
            val file = viewContainer.psiElement!!.containingFile
            val activityComposables =
                ActivityParser.getInstance(project)?.getComposablesForFile(project, file)
            composables.addAll(ComponentUtils.predefinedComposables)
            activityComposables?.let { composables.addAll(it) }
            AddViewDialog(composables, viewContainer, this::create).show()
        }
        catch (_: Exception) { }
    }
}
