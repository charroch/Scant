import sbt._
import Keys._
import AndroidKeys._

object Scant extends Build {

  lazy val root = Project(id = "Scant",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      resolvers ++= Seq(
        "snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
        "releases" at "http://oss.sonatype.org/content/repositories/releases"
      ),
      libraryDependencies ++= Seq(
        "com.google.android" % "support-v4" % "r6",
        "org.scalatest" %% "scalatest" % "1.8-SNAPSHOT",
        "com.google.android" % "android" % "4.0.1.2" % "provided",
        "com.google.android" % "android-test" % "4.0.1.2" % "provided"
      )
    )
  )

  object General {
    val settings = Defaults.defaultSettings ++ Seq(
      resolvers ++= Seq(
        "snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
        "releases" at "http://oss.sonatype.org/content/repositories/releases"
      ),
      name := "SantAndroidTesting",
      version := "0.1",
      versionCode := 0,
      scalaVersion := "2.9.1",
      platformName in Android := "android-10"
    )

    val proguardSettings = Seq(
      useProguard in Android := false
    )

    lazy val fullAndroidSettings =
      General.settings ++
        AndroidProject.androidSettings ++
        TypedResources.settings ++
        proguardSettings ++
        AndroidManifestGenerator.settings ++ Seq(
        libraryDependencies += "org.scalatest" %% "scalatest" % "1.8-SNAPSHOT" % "test"
      )
  }

  lazy val main = Project(
    "ScantAndroid",
    file("src/example/scant"),
    settings = General.fullAndroidSettings
  )

  def scalaTestLogger(s: Logger): AndroidTest.TestParser = {
    i =>
      io.Source.fromInputStream(i).getLines.foreach(s.info(_))
  }

  lazy val tests = Project(
    "ScantAndroidTests",
    file("src/example/scant/tests"),
    settings = General.settings ++
      AndroidTest.settings ++
      General.proguardSettings ++ Seq(
      name := "SantAndroidTestingTests",
      instrumentationRunner in Android := "org.scalatest.tools.SpecRunner",
      testOutputParser in Android <<= streams map {
        (s: TaskStreams) => Some(scalaTestLogger(s.log))
      },
      dxInputs in Android ~= {
        (inputs: Seq[File]) =>
          inputs.filterNot(n => n.getName.contains("specs") || n.getName.contains("scalatest"))
      }
    )
  ) dependsOn(main, root)

}