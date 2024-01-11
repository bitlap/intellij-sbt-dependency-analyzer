package bitlap.sbt.analyzer.parser;

import scala.collection.mutable.ListBuffer

final class Graph(size: Int) {

  private val graph = Array.fill(size)(ListBuffer[Int]())

  def addEdge(v: Int, w: Int): Unit = {
    graph(v) += w
  }

  def dfs(v: Int): ListBuffer[Int] = {
    val visited = Array.fill(size + 1)(false)
    val res     = ListBuffer[Int]()
    helper(v, visited, res)
    res
  }

  private def helper(v: Int, visited: Array[Boolean], res: ListBuffer[Int]): Unit = {
    visited(v) = true

    res += v

    graph(v).foreach(n => {
      if (!visited(n))
        helper(n, visited, res)
    })
  }
}
