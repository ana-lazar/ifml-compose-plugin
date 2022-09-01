package services.modifiers

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.PsiTestUtil
import editor.Diagram
import editor.nodes.ViewContainer
import services.ContentManager
import services.parsers.ManifestParser
import utils.WriteActionDispatcher

class ViewRemover(private val project: Project, private val diagram: Diagram) {
    private val dispatcher = WriteActionDispatcher(project)

    fun remove(viewContainer: ViewContainer) {
        if (viewContainer.psiElement is PsiFile) {
            val psiFile = viewContainer.psiElement as PsiFile
            ContentManager.getInstance(project)?.handleAction(psiFile.virtualFile, ContentManager.Action.CLOSE)
            val manifestFile = ManifestParser.getInstance(project)?.findAndroidManifestFile(project)
            if (manifestFile is XmlFile) {
                val applicationTag = manifestFile.document?.rootTag?.findFirstSubTag("application")
                val activityElement = applicationTag?.findSubTags("activity")?.single { element ->
                    element.attributes.any {
                        val activityNameTokens = it.value?.split(".")?.toTypedArray()
                        val activityName = activityNameTokens?.get(activityNameTokens.size - 1)
                        it.name == "android:name" && "$activityName.kt" == psiFile.name } }
                val closeFile = Runnable {
                    FileEditorManager.getInstance(project).closeFile(psiFile.virtualFile)
                    VirtualFileManager.getInstance().refreshWithoutFileWatcher(false)
                }
                ApplicationManager.getApplication().invokeLater(closeFile)
                dispatcher.dispatch {
                    activityElement?.delete()
                    psiFile.delete()
                }
            }
        }
        else {
            dispatcher.dispatch {
                val parent = viewContainer.psiElement?.parent
                viewContainer.psiElement?.delete()
                parent?.containingFile?.virtualFile?.let { ContentManager.getInstance(project)?.handleAction(it, ContentManager.Action.UPDATE) }
            }
        }
    }
}
