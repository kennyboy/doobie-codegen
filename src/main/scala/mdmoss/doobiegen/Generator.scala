package mdmoss.doobiegen

import mdmoss.doobiegen.output.File
import mdmoss.doobiegen.sql.Table
import Analysis._

class Generator(analysis: Analysis) {

  val a = analysis
  val db = analysis.model
  val target = analysis.target
  val tr = target.testDb

  def gen: Seq[File] = {
    /* First aim - objects for each database table */

    val tableFiles = db.tables.map { t =>

      val contents =
        s"""package ${a.targetPackage(t)}
            |
            |/* Todo handle imports better */
            |import doobie.imports._
            |import java.sql.Timestamp
            |
            |object ${a.targetObject(t)} {
            |
            |  ${genPkNewType(t)}
            |
            |  ${genRowType(t)}
            |
            |  ${ppFunctionDef(a.insert(t).fn)}
            |
            |  ${ppFunctionDef(a.create(t).fn)}
            |
            |  ${a.get(t).map { g =>
                  ppFunctionDef(g.inner) + "\n" +
                  ppFunctionDef(g.outer)
                }.getOrElse("")}
            |
            |  ${a.find(t).map { f =>
                  ppFunctionDef(f.inner) + "\n" +
                  ppFunctionDef(f.outer)
               }.getOrElse("")}
            |
            |  ${ppFunctionDef(a.allUnbounded(t).inner)}
            |  ${ppFunctionDef(a.allUnbounded(t).outer)}
            |
            |}
         """.stripMargin


      File(
        a.targetPackage(t),
        a.targetObject(t) + ".scala",
        contents,
        isTest = false
      )
    }

    /* We're going to follow up with tests */
    val testFiles = db.tables.map { t =>
      val contents =
        s"""package ${a.targetPackage(t)}
            |
            |/* Todo handle imports better */
            |import doobie.imports._
            |import java.sql.Timestamp
            |import org.specs2.mutable.Specification
            |import scalaz.concurrent.Task
            |import doobie.contrib.specs2.analysisspec.AnalysisSpec
            |import scalaz.NonEmptyList
            |
            |object ${a.targetObject(t)}Spec extends Specification with AnalysisSpec {
            |
            |  val transactor = DriverManagerTransactor[Task]("${tr.driver}", "${tr.url}", "${tr.username}", "${tr.password}")
            |
            |  ${checkTest(t, a.insert(t).fn)}
            |
            |  ${a.get(t).map { g => checkTest(t, g.inner)}.getOrElse("")}
            |
            |  ${a.find(t).map { f => checkTest(t, f.inner)}.getOrElse("")}
            |
            |  ${checkTest(t, a.allUnbounded(t).inner)}
            |}
         """.stripMargin

      File(
        a.targetPackage(t),
        a.targetObject(t) + "Spec.scala",
        contents,
        isTest = true
      )
    }

    tableFiles ++ testFiles
  }

  def genPkNewType(table: Table): String = {
    a.pkNewType(table).map { pk =>
      s"case class ${pk._2.symbol}(${pk._1.map(f => s"${f.scalaName}: ${f.scalaType.symbol}").mkString(", ")})"
    }.getOrElse("")
  }

  def genRowType(table: Table): String = {
    val row = a.rowNewType(table)
    s"case class ${row._2.symbol}(${row._1.map(f => s"${f.scalaName}: ${f.scalaType.symbol}").mkString(", ")})"
  }

  def genInsert(table: Table): String = {
    val in = a.insert(table)
    ppFunctionDef(in.fn)
  }

  def genCreate(table: Table): String = {
    val create = a.create(table)
    ppFunctionDef(create.fn)
  }

  def ppFunctionDef(fn: FunctionDef): String = {
    val scope = fn.privatePkg.map { p => s"private[$p]" }.getOrElse("")
    val params = fn.params.map(f => s"${f.name}: ${f.`type`.symbol}").mkString(", ")

    s"""$scope def ${fn.name}($params): ${fn.returnType} = {
       |${fn.body}
       |}
     """.stripMargin

  }

  def genGet(table: Table): String = {
    a.get(table).map { get =>
      s"""${ppFunctionDef(get.inner)}
         |${ppFunctionDef(get.outer)}
       """.stripMargin
    }.getOrElse("")
  }

  def checkTest(table: Table, fn: FunctionDef): String = {
    val obj = a.targetObject(table)
    s"""check($obj.${fn.name}(${fn.params.map(_.`type`.arb).mkString(", ")}))"""
  }

}
