package listeners

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile
import services.ContentManager

class FileEditorListenerImpl : FileEditorManagerListener {
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        super.fileOpened(source, file)

        try {
            val project = source.project
            ContentManager.getInstance(project)?.handleAction(file, ContentManager.Action.OPEN)
        }
        catch (_: Exception) { }
    }

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        super.fileClosed(source, file)

        try {
            val project = source.project
            ContentManager.getInstance(project)?.handleAction(file, ContentManager.Action.CLOSE)
        }
        catch (_: Exception) { }
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        try {
            if (event.newFile == null) {
                return
            }
            val project = event.manager.project
            ContentManager.getInstance(project)?.handleAction(event.newFile, ContentManager.Action.OPEN)
        }
        catch (_: Exception) { }
    }
}
