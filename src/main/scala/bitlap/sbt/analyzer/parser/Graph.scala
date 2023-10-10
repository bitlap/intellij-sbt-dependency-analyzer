package bitlap.sbt.analyzer.parser;

import scala.collection.mutable.ListBuffer

class Graph(V: Int) {

  private val adj = Array.fill(V)(ListBuffer[Int]())

  def addEdge(v: Int, w: Int): Unit = {
    adj(v) += w
  }

  def DFS(v: Int): ListBuffer[Int] = {
    val visited = Array.fill(V + 1)(false)
    val res     = ListBuffer[Int]()
    DFSUtil(v, visited, res)
    res
  }

  private def DFSUtil(v: Int, visited: Array[Boolean], res: ListBuffer[Int]): Unit = {
    visited(v) = true

    res += v

    adj(v).foreach(n => {
      if (!visited(n))
        DFSUtil(n, visited, res)
    })
  }
}
