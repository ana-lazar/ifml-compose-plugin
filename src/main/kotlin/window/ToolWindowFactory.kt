package window

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import listeners.CaretListenerImpl
import services.ContentManager

class ToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        ContentManager
            .getInstance(project)?.setViewHolder(project, toolWindow)
        EditorFactory.getInstance()
            .eventMulticaster
            .addCaretListener(CaretListenerImpl(project), ApplicationManager.getApplication())
    }
}
