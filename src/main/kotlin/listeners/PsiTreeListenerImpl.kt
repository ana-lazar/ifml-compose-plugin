package listeners

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.psi.PsiTreeChangeListener
import services.ContentManager

class PsiTreeListenerImpl : PsiTreeChangeListener {
    override fun childrenChanged(event: PsiTreeChangeEvent) {
        try {
            val project = event.file?.project
            ApplicationManager.getApplication().runWriteAction {
                event.file?.let {
                    if (project != null) {
                        ContentManager.getInstance(project)?.handleAction(it.virtualFile, ContentManager.Action.UPDATE)
                    }
                }
            }
        }
        catch (_: Exception) { }
    }

    override fun beforeChildAddition(event: PsiTreeChangeEvent) { }

    override fun beforeChildRemoval(event: PsiTreeChangeEvent) { }

    override fun beforeChildReplacement(event: PsiTreeChangeEvent) { }

    override fun beforeChildMovement(event: PsiTreeChangeEvent) { }

    override fun beforeChildrenChange(event: PsiTreeChangeEvent) { }

    override fun beforePropertyChange(event: PsiTreeChangeEvent) { }

    override fun childAdded(event: PsiTreeChangeEvent) { }

    override fun childRemoved(event: PsiTreeChangeEvent) { }

    override fun childReplaced(event: PsiTreeChangeEvent) { }

    override fun childMoved(event: PsiTreeChangeEvent) { }

    override fun propertyChanged(event: PsiTreeChangeEvent) { }
}
