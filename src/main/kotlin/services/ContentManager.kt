package services

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import org.apache.commons.codec.binary.Hex
import java.security.MessageDigest
import persistence.DiagramRepository
import services.parsers.ManifestParser
import views.ActivityPanel
import views.ProjectPanel

class ContentManager {
    private var project: Project? = null
    private var viewHolder: ToolWindow? = null
    private var activities: MutableList<ActivityViewContent> = mutableListOf()
    private var diagramRepository: DiagramRepository? = null
    private var projectPanel: ProjectPanel? = null

    fun setViewHolder(currentProject: Project?, toolWindow: ToolWindow) {
        try {
            project = currentProject
            diagramRepository = project?.let { DiagramRepository(it) }

            // add project content window
            projectPanel = ProjectPanel(project!!, diagramRepository)
            val content = ContentFactory.SERVICE.getInstance().createContent(projectPanel, "Project", false)
            toolWindow.contentManager.addContent(content)

            DumbService.getInstance(project!!).runWhenSmart {
                // add open files windows
                for (editor in FileEditorManager.getInstance(project!!).allEditors) {
                    handleAction(editor.file!!, Action.OPEN)
                }

                // set activity view holder
                for (activity in activities) {
                    toolWindow.contentManager.addContent(activity.content)
                }

                // set selected content
                val selectedFile = FileEditorManager.getInstance(project!!).selectedEditor?.file
                if (selectedFile?.canonicalPath != null) {
                    val thisActivityFilePath = selectedFile.canonicalPath!!
                    val index = findActivityContentIndex(thisActivityFilePath)
                    if (index != null) {
                        val activityContents = activities[index]
                        toolWindow.contentManager.setSelectedContent(activityContents.content)
                    }
                }
            }

            viewHolder = toolWindow
        }
        catch (_: Exception) { }
    }

    fun handleAction(file: VirtualFile, action: Action) {
        try {
            when (action) {
                Action.OPEN -> {
                    isActivityExecutor(file) { psiFile -> openOrReloadActivity(psiFile) }
                }
                Action.CLOSE -> {
                    isActivityExecutor(file) { psiFile -> closeActivity(psiFile) }
                }
                Action.UPDATE -> {
                    isActivityExecutor(file) { psiFile -> updateActivity(psiFile) }
                }
            }
        }
        catch (_: Exception) { }
    }

    private fun isActivityExecutor(file: VirtualFile, execute: (PsiFile) -> Unit) {
        project?.let {
            DumbService.getInstance(it).runWhenSmart {
                val psiFile = PsiManager.getInstance(project!!).findFile(file)
                if (psiFile != null) {
                    val isActivity = ManifestParser.getInstance(project!!)?.isActivityFile(psiFile, project!!)
                    if (isActivity == true) {
                        execute(psiFile)
                    }
                }
            }
        }
    }

    private fun openOrReloadActivity(file: PsiFile) {
        // find file content index in tool window contents
        val thisActivityFilePath = file.virtualFile.canonicalPath!!
        val index = findActivityContentIndex(thisActivityFilePath)
        // check if content is existent and otherwise
        if (index != null) {
            val activityContents = activities[index]
            val newContentsDigest = computePsiFileDigest(file)
            if (newContentsDigest == activityContents.digest) {
                viewHolder?.contentManager?.setSelectedContent(activityContents.content)
            } else {
                activities.removeAt(index)
                viewHolder?.contentManager?.removeContent(activityContents.content, true)
                createAndAddNewActivityViewContents(file, index)
            }
        } else {
            createAndAddNewActivityViewContents(file, index)
        }
    }

    private fun closeActivity(file: PsiFile?) {
        val thisActivityFilePath = file?.virtualFile?.canonicalPath!!
        val index = findActivityContentIndex(thisActivityFilePath) ?: return
        val content = activities[index]
        activities.remove(content)
        viewHolder?.contentManager?.removeContent(content.content, true)
    }

    private fun updateActivity(file: PsiFile) {
        // find file content index in tool window contents
        val thisActivityFilePath = file.virtualFile.canonicalPath!!
        val index = findActivityContentIndex(thisActivityFilePath)
        // check if content is existent and otherwise
        if (index != null) {
            val activityContents = activities[index]
            try {
                activityContents.panel.buildDiagram()
                projectPanel?.buildDiagrams()
                viewHolder?.contentManager?.setSelectedContent(activityContents.content)
            }
            catch (_: Exception) { }
        } else {
            createAndAddNewActivityViewContents(file, null)
        }
    }

    fun selectElement(element: PsiElement) {
        val file = element.containingFile
        val thisActivityFilePath = file.virtualFile.canonicalPath!!
        val index = findActivityContentIndex(thisActivityFilePath)
        // check if content is existent and otherwise
        if (index != null) {
            val activityContents = activities[index]
            activityContents.panel.selectElement(element)
        }
    }

    private fun findActivityContentIndex(path: String): Int? {
        for (i in activities.indices) {
            val filePath = activities[i].psiFile.containingFile.virtualFile.canonicalPath
            if (filePath == path) {
                return i
            }
        }
        return null
    }

    private fun computePsiFileDigest(file: PsiFile): String {
        val fileDigest = MessageDigest.getInstance("SHA-256")
        fileDigest.update(VfsUtil.loadBytes(file.virtualFile))
        return Hex.encodeHexString(fileDigest.digest())
    }

    private fun createAndAddNewActivityViewContents(file: PsiFile, index: Int?) {
        // create content for file
        val content = createActivityViewContent(file)
        if (index != null) {
            activities.add(index, content)
        } else {
            activities.add(content)
        }
        // add and select new content
        if (viewHolder != null) {
            if (index != null) {
                viewHolder!!.contentManager.addContent(content.content, index)
            } else {
                viewHolder!!.contentManager.addContent(content.content)
            }
            viewHolder!!.contentManager.setSelectedContent(content.content, true)
        }
    }

    private fun createActivityViewContent(file: PsiFile): ActivityViewContent {
        val panel = ActivityPanel(project!!, file, diagramRepository)
        val content = ContentFactory.SERVICE.getInstance().createContent(panel, file.name, false)
        return ActivityViewContent(file, panel, content, computePsiFileDigest(file))
    }

    internal class ActivityViewContent(val psiFile: PsiFile, val panel: ActivityPanel, val content: Content, val digest: String)

    enum class Action {
        OPEN,
        CLOSE,
        UPDATE
    }

    companion object {
        fun getInstance(project: Project): ContentManager? {
            return project.getService(ContentManager::class.java)
        }
    }
}
