package utils

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project

private const val COMMAND_NAME = "IFML Diagrams"
private const val GROUP_ID = "IFML_DIAGRAMS_ID"

class WriteActionDispatcher(val project: Project) {
    fun dispatch(action: () -> Unit) =
        WriteCommandAction.runWriteCommandAction(project, COMMAND_NAME, GROUP_ID, {
            action()
        })
}
