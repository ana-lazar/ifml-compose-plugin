package services.modifiers

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.XmlElementFactory
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.PsiTestUtil
import org.jetbrains.kotlin.idea.KotlinLanguage
import utils.CodeTemplates
import utils.WriteActionDispatcher
import services.parsers.ManifestParser

class ActivityCreator(private val project: Project) {
    private val dispatcher = WriteActionDispatcher(project)

    fun create(name: String) {
        val selectedFile = FileEditorManager.getInstance(project).selectedEditor?.file
        selectedFile?.let {
            val directory = PsiManager.getInstance(project).findFile(selectedFile)?.containingDirectory
            if (directory != null) {
                val manifestFile = ManifestParser.getInstance(project)?.findAndroidManifestFile(project)
                if (manifestFile is XmlFile) {
                    val applicationTag = manifestFile.document?.rootTag?.findFirstSubTag("application")
                    val tagText = CodeTemplates.ACTIVITY_TAG.replace("%activityPath%", "${directory.name}.${name}")
                    val activityElement = XmlElementFactory.getInstance(project).createTagFromText(tagText)
                    val text = CodeTemplates.ACTIVITY_FILE.replace("%activityName%", name).replace("%packageName%", directory.name)
                    var psiFile = PsiFileFactory.getInstance(project).createFileFromText("${name}Activity.kt", KotlinLanguage.INSTANCE, text)
                    psiFile = CodeStyleManager.getInstance(project).reformat(psiFile) as PsiFile
                    dispatcher.dispatch {
                        applicationTag?.add(activityElement)
                        val addedElement = directory.add(psiFile) as PsiFile
                        val openFile = Runnable {
                            FileEditorManager.getInstance(project).openFile(addedElement.virtualFile, true)
                        }
                        ApplicationManager.getApplication().invokeLater(openFile)
                    }
                }
            }
        }
    }
}
