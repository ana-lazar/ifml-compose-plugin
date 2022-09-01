package services.modifiers

import com.intellij.openapi.project.Project
import editor.nodes.ViewContainer
import utils.WriteActionDispatcher

class ParameterModifier(private val project: Project) {
    private val dispatcher = WriteActionDispatcher(project)

    fun modify(newName: String, oldName: String, viewContainer: ViewContainer) {
        println(newName)
        println(oldName)
        println(viewContainer)
    }
}
