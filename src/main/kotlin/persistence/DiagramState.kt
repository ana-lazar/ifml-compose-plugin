package persistence

import java.io.Serializable

data class DiagramState(
    var diagrams: MutableMap<String, DiagramData> = mutableMapOf()
) : Serializable
