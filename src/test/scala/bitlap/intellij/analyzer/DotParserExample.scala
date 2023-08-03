package bitlap.intellij.analyzer

/** @author
 *    梦境迷离
 *  @version 1.0,2023/8/3
 */
object DotParserExample extends App {
  val relations = DotParser.getDependencyRelations(getClass.getClassLoader.getResource("test.dot").getFile)
  println(relations)
}
