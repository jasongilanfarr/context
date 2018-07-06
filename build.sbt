enablePlugins(GitVersioning, GitBranchPrompt)


val commonSettings = Seq(
  organization := "org.thisamericandream",
  libraryDependencies := Seq(
    "org.scala-lang" % "scala-library" % scalaVersion.value,
    "org.slf4j" % "slf4j-api" % "1.7.25",
    "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
    "org.scalatest" %% "scalatest" % "3.0.5" % Test
  ),
  scalaVersion := "2.12.6",
  parallelExecution in Test := true,
  scalacOptions := Seq(
    "-deprecation", "-Xlint", "-encoding", "utf-8", "-unchecked", "-Xcheckinit",
    "-Xfatal-warnings", "-Yno-adapted-args", "-Ywarn-dead-code", "-Ywarn-extra-implicit",
    "-Xlint:adapted-args", "-Xlint:by-name-right-associative", "-Xlint:constant",
    "-Xlint:delayedinit-select", "-Xlint:doc-detached", "-Xlint:inaccessible",
    "-Xlint:infer-any", "-Xlint:missing-interpolator", "-Xlint:nullary-override",
    "-Xlint:nullary-unit", "-Xlint:option-implicit", "-Xlint:package-object-classes",
    "-Xlint:poly-implicit-overload", "-Xlint:private-shadow", "-Xlint:stars-align",
    "-Xlint:type-parameter-shadow", "-Xlint:unsound-match",
    "-Ywarn-inaccessible", "-Ywarn-infer-any", "-Ywarn-nullary-override", "-Ywarn-nullary-unit",
    "-Ywarn-numeric-widen", "-Ywarn-unused:implicits", "-Ywarn-unused:imports", "-Ywarn-unused:locals",
    "-Ywarn-unused:params", "-Ywarn-unused:patvars", "-Ywarn-unused:privates"
  )
)

lazy val context = (project in file("context"))
  .settings(commonSettings)

lazy val `context-akka` = (project in file("context-akka"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.5.13",
      "com.typesafe.akka" %% "akka-testkit" % "2.5.13" % Test
    )
  )
  .dependsOn(context % "compile->compile;test->test")


lazy val root = (project in file("."))
  .aggregate(context, `context-akka`)
