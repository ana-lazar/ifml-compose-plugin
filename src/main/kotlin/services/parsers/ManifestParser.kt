package services.parsers

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import org.apache.xerces.parsers.DOMParser
import org.jetbrains.kotlin.idea.caches.resolve.util.isInDumbMode
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.w3c.dom.Element
import org.w3c.dom.NodeList

class ManifestParser {
    fun findAndroidManifestFile(project: Project): PsiFile? {
        val files = FilenameIndex.getFilesByName(
            project,
            "AndroidManifest.xml",
            GlobalSearchScope.projectScope(project)
        )
        return if (files.isNotEmpty()) files[0] else null
    }

    private fun findActivityNameFromElement(activityElement: Element): String {
        val fullyQualifiedActivityName = activityElement.getAttribute("android:name")
        val activityNameTokens = fullyQualifiedActivityName.split(".").toTypedArray()
        val activityName = activityNameTokens[activityNameTokens.size - 1]
        return "$activityName.kt"
    }

    private fun listActivityFiles(project: Project): NodeList? {
        val androidManifestFile: PsiFile = findAndroidManifestFile(project) ?: return null
        val parser = DOMParser()
        parser.parse(androidManifestFile.virtualFile.path)
        val androidManifestDocument = parser.document
        return androidManifestDocument.getElementsByTagName("activity")
    }

    fun findActivitiesByProject(project: Project): List<String> {
        val activityNames = mutableListOf<String>()
        val nodeList = listActivityFiles(project)
        if (nodeList != null) {
            for (i in 0 until nodeList.length) {
                val node = nodeList.item(i)
                if (node is Element) {
                    val files = FilenameIndex.getFilesByName(
                        project,
                        findActivityNameFromElement(node),
                        GlobalSearchScope.projectScope(project)
                    )
                    if (files.isNotEmpty()) {
                        val name = files[0].name.split(".").toTypedArray()[0]
                        activityNames.add(name)
                    }
                }
            }
        }
        return activityNames
    }

    fun getNameFromKtCallExpression(callExpr: KtExpression): String {
        val id = (callExpr.firstChild as KtReferenceExpression).firstChild as PsiElement
        return id.text
    }

    fun isActivityFile(file: PsiFile, project: Project): Boolean {
        val androidManifestFile = findAndroidManifestFile(project) ?: return false
        val parser = DOMParser()
        parser.parse(androidManifestFile.virtualFile.path)
        val androidManifestDocument = parser.document
        val elements = androidManifestDocument.getElementsByTagName("activity")
        if (elements != null) {
            for (i in 0 until elements.length) {
                val element = elements.item(i)
                if (element is Element && findActivityNameFromElement(element) == file.name) {
                    return true
                }
            }
        }
        return false
    }

    companion object {
        fun getInstance(project: Project): ManifestParser? {
            return project.getService(ManifestParser::class.java)
        }
    }
}
