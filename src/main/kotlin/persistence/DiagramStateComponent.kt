package persistence

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import java.io.Serializable

@State(
    name = "DiagramState",
    storages = [Storage(value = "diagramPositions.xml")]
)
class DiagramStateComponent : Serializable, PersistentStateComponent<DiagramStateComponent> {
    var diagramState: DiagramState = DiagramState()

    override fun getState(): DiagramStateComponent = this

    override fun loadState(state: DiagramStateComponent) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(project: Project): DiagramStateComponent {
            return ServiceManager.getService(project, DiagramStateComponent::class.java)
        }
    }
}
