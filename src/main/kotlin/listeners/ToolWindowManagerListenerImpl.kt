package listeners

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

class ToolWindowManagerListenerImpl(private val project: Project) : ToolWindowManagerListener {
    override fun stateChanged(toolWindowManager: ToolWindowManager) {
        super.stateChanged(toolWindowManager)

        try {
            if (project.isDisposed) {
                return
            }

            if (toolWindowManager.activeToolWindowId == "IFML") {
                val toolWindow = toolWindowManager.getToolWindow("IFML")
                val selectedContent = toolWindow?.contentManager?.selectedContent?.tabName
                val selectedEditor = FileEditorManager.getInstance(project).selectedEditor?.name
                if (selectedContent != selectedEditor) {
                    val files = FilenameIndex.getFilesByName(
                        project,
                        toolWindow?.contentManager?.selectedContent?.tabName ?: "",
                        GlobalSearchScope.projectScope(project)
                    )
                    if (files.isNotEmpty()) {
                        val openFile = Runnable {
                            if (!project.isDisposed) {
                                FileEditorManager.getInstance(project).openFile(files[0].virtualFile, true)
                            }
                        }
                        ApplicationManager.getApplication().invokeLater(openFile)
                    }
                }
            }
        }
        catch (_: Exception) { }
    }
}
