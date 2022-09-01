package persistence

import java.io.Serializable

data class NodeData(
    var id: String = "",
    var x: Int = 0,
    var y: Int = 0,
    var height: Int = 0,
    var width: Int = 0,
    var path: String = "",
    var children: MutableList<NodeData> = mutableListOf()
) : Serializable

data class DiagramData(var nodes: MutableList<NodeData> = mutableListOf()) : Serializable
