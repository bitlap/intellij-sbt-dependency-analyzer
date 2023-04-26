package bitlap.intellij.analyzer.model

import com.intellij.ui.ColorUtil.*
import java.awt.*
import com.intellij.ui.JBColor
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.UIUtil

/** @author
 *    梦境迷离
 *  @version 1.0,2023/4/26
 */
final class HighlightingTree extends Tree {
  setOpaque(false)

  override def getFileColorFor(`object`: AnyRef): Color =
    `object` match {
      case value: Dependency if value.highlight =>
        if (UIUtil.isUnderDarcula)
          darker(JBColor.CYAN, 8)
        else softer(Color.CYAN)
      case _ => super.getFileColorFor(`object`)
    }

  override def isFileColorsEnabled = true

}
