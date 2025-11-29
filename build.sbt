import sbtcrossproject.CrossPlugin.autoImport.*
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.*
import sbt.Keys.*
import sbt.*

val commonSettings = Seq(
  scalaVersion := "3.7.2",
  scalacOptions ++= List("-deprecation", "-feature", "-Xfatal-warnings")
)

val webappLibRepo = uri("https://gitlab.epfl.ch/cs214/ul/webapp-lib.git#v0.27.0")

lazy val client: ProjectReference = ProjectRef(webappLibRepo, "webappLibJS")
lazy val server: ProjectReference = ProjectRef(webappLibRepo, "webappLibJVM")

lazy val copyJsTask = TaskKey[Unit]("copyJsTask", "Copy javascript files to server static directory")
lazy val assemble   = TaskKey[Unit]("assemble", "Package the project into a single uber-jar")

def copyJsTaskFactory(fastOptimized: Boolean) = Def.task {
  val sv = scalaVersion.value
  val bundleDir =
    if (fastOptimized) "appemojionary-fastopt"
    else "appemojionary-opt"

  val inDir  = baseDirectory.value / s"apps/js/target/scala-$sv/$bundleDir/"
  val outDir = baseDirectory.value / "apps/jvm/src/main/resources/www/static/"
  IO.createDirectory(outDir)

  def copyIfExists(name: String): Unit = {
    val src = inDir / name
    val dst = outDir / name
    if (src.exists) IO.copyFile(src, dst)
  }

  copyIfExists("main.js")
  copyIfExists("main.js.map")
}


lazy val appEmojionary = (crossProject(JVMPlatform, JSPlatform) in file("./apps"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "ujson" % "4.3.0"
    )
  )
  .jsSettings(
  scalaJSUseMainModuleInitializer := true,
  mainClass := Some("apps.MainJS"),
  test / aggregate := false,
  Test / test := {},
  Test / testOnly := {},
  libraryDependencies ++= Seq(
    "org.scala-lang" %% "scala3-library" % "3.7.2",
    "org.scala-js" %%% "scalajs-dom" % "2.8.1",
    "com.lihaoyi" %%% "scalatags" % "0.13.1"
  )
)

  .jvmSettings(
    assembly / assemblyJarName := "webapp.jar",
    assembly / assemblyMergeStrategy := { case _ => sbtassembly.MergeStrategy.first },
    assembly / mainClass := Some("apps.MainJVM"),
    run / fork := true,
    Global / cancelable := true,
    libraryDependencies ++= Seq(
      "org.scala-lang" %% "toolkit-test" % "0.7.0" % Test
    )
  )

lazy val appEmojionaryJS  = appEmojionary.js.dependsOn(client)
lazy val appEmojionaryJVM = appEmojionary.jvm.dependsOn(server % "compile->compile;test->test")

lazy val ul2025app93 = (project in file("."))
  .aggregate(appEmojionaryJS, appEmojionaryJVM)
  .settings(commonSettings)
  .settings(
    copyJsTask := copyJsTaskFactory(true).value,

    Test / test := Def.sequential(
      (Test / testOnly).toTask("")
    ).value,

    assemble := Def.sequential(
      Compile / compile,
      (appEmojionaryJS / Compile / fullLinkJS).toTask,
      copyJsTaskFactory(false),
      (appEmojionaryJVM / assembly).toTask
    ).value,

    Compile / run := Def.sequential(
      Compile / compile,
      (appEmojionaryJS / Compile / fastLinkJS).toTask,
      copyJsTask,
      (appEmojionaryJVM / Compile / run).toTask("")
    ).value
  )
