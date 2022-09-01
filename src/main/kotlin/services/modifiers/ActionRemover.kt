package services.modifiers

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtPsiFactory
import editor.nodes.Action
import utils.WriteActionDispatcher

class ActionRemover(private val project: Project) {
    private val dispatcher = WriteActionDispatcher(project)

    fun remove(viewContainer: Action) {
        val factory = KtPsiFactory(project)
        val newElement = factory.createLambdaExpression("", "")
        dispatcher.dispatch {
            viewContainer.psiElement?.replace(newElement)
        }
    }
}
