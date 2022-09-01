package services

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

internal class EditorCaretManager(private val project: Project) {
    private var _shouldMoveCaret = true

    fun moveEditorCaret(element: PsiElement?) {
        try {
            if (element == null) return
            if (shouldMoveCaret()) {
                val file = element.containingFile.virtualFile
                val fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(file)
                var editor: Editor? = null
                if (fileEditor is TextEditor) {
                    editor = fileEditor.editor
                }
                val textOffset = element.textOffset
                if (textOffset < file.length) {
                    editor?.caretModel?.moveToOffset(textOffset)
                    editor?.scrollingModel?.scrollToCaret(ScrollType.MAKE_VISIBLE)
                }
            }
            _shouldMoveCaret = true
        }
        catch (_: Exception) { }
    }

    private fun shouldMoveCaret(): Boolean {
        return _shouldMoveCaret
    }

    companion object {
        fun getInstance(project: Project): EditorCaretManager? {
            return project.getService(EditorCaretManager::class.java)
        }
    }
}
