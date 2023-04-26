package bitlap.intellij.analyzer.model

import bitlap.intellij.analyzer.DefaultArtifactVersion
import java.util.{ Collections, Comparator }

/** @author
 *    梦境迷离
 *  @version 1.0,2023/4/26
 */
final case class ListNode(s: (String, List[Dependency])):
  import ListNode._

  val key: String             = s._1
  val value: List[Dependency] = s._2
  val maxVersion: String      = sortByVersion(s._2)

  override def toString = key

  override def equals(o: Any): Boolean =
    if (this eq o.asInstanceOf[AnyRef]) return true
    if (o == null || (getClass ne o.getClass)) return false
    val that = o.asInstanceOf[ListNode]
    if (
      if (key != null) !(key == that.key)
      else that.key != null
    ) return false
    true

  override def hashCode = if (key != null) key.hashCode else 0

end ListNode

object ListNode:
  def sortByVersion(value: List[Dependency]): String =
    value.sortWith { (o1, o2) =>
      val version  = new DefaultArtifactVersion(o1.version)
      val version1 = new DefaultArtifactVersion(o2.version)
      version1.compareTo(version) > 0
    }.headOption.map(_.version).getOrElse("Unknown")
