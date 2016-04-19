val commonSettings = Seq(
  name := "doobiegen",
  organization := "mdmoss",
  scalaVersion := "2.11.7",
  scalacOptions ++= Seq("-deprecation", "-feature"),
  scalacOptions in Test ++= Seq("-Yrangepos")
)

lazy val main = (project in file(""))
  .settings(commonSettings:_*)
  .settings(
    libraryDependencies += "org.parboiled" %% "parboiled" % "2.1.2"
  )

lazy val out = (project in file("out"))
  .settings(commonSettings:_*)
  .settings(
    resolvers += "tpolecat" at "http://dl.bintray.com/tpolecat/maven",
    libraryDependencies ++= Seq(
      "org.tpolecat"  %% "doobie-core"               % "0.2.3",
      "org.tpolecat"  %% "doobie-contrib-postgresql" % "0.2.3",
      "org.tpolecat"  %% "doobie-contrib-specs2"     % "0.2.3" % "test",
      "org.specs2"    %% "specs2-core"               % "3.6.3" % "test"
    )
  )
