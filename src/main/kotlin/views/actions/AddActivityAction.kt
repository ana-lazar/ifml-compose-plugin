package views.actions

import com.intellij.openapi.project.Project
import javax.swing.*
import editor.Diagram
import services.modifiers.ActivityCreator
import views.dialogs.AddActivityDialog

class AddActivityAction(name: String, project: Project, diagram: Diagram) : PanelAction(name, project, diagram) {
    private val creator = ActivityCreator(project)

    fun create(name: String) {
        creator.create(name)
    }

    override fun execute(x: Int, y: Int, panel: JPanel) {
        try {
            AddActivityDialog(this::create).show()
        }
        catch (_: Exception) { }
    }
}
