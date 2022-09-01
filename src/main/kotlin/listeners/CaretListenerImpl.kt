package listeners

import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtCallExpression
import services.ContentManager

class CaretListenerImpl(private val project: Project) : CaretListener {
    override fun caretPositionChanged(event: CaretEvent) {
        super.caretPositionChanged(event)

        try {
            if (project.isDisposed) {
                return
            }

            val editor = event.editor
            val psiFile = PsiDocumentManager.getInstance(editor.project!!).getPsiFile(editor.document)

            var elementAtCaret: PsiElement? = null
            if (psiFile != null) {
                val viewProvider = psiFile.viewProvider
                elementAtCaret = viewProvider.findElementAt(editor.caretModel.offset, psiFile.language)
            }

            if (elementAtCaret != null) {
                val firstCall = PsiTreeUtil.getParentOfType(elementAtCaret, KtCallExpression::class.java)
                if (firstCall != null) {
                    ContentManager.getInstance(project)?.selectElement(firstCall)
                }
                else {
                    ContentManager.getInstance(project)?.selectElement(elementAtCaret.containingFile)
                }
            }
        }
        catch (_: Exception) { }
    }
}