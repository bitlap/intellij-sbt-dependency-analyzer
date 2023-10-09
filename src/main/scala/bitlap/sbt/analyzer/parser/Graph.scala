package bitlap.sbt.analyzer.parser;

import java.util
import java.util.List as JList
import java.util.function.Consumer

class Graph(V: Integer) {

  private val adj = Array.fill(V)(new util.LinkedList[Integer]())

  def addEdge(v: Integer, w: Integer): Unit = {
    adj(v).add(w)
  }

  def DFS(v: Integer): JList[Integer] = {
    val visited = Array.fill(V + 1)(false)
    val res     = new util.ArrayList[Integer]()
    DFSUtil(v, visited, res)
    res
  }

  private def DFSUtil(v: Integer, visited: Array[Boolean], res: JList[Integer]): Unit = {
    visited(v) = true

    res.add(v)

    adj(v).forEach((n: Integer) => {
      if (!visited(n))
        DFSUtil(n, visited, res)
    })
  }
}
