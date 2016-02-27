name := "everylucky"

version := "1.0.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.3.0"

enablePlugins(DockerPlugin)

enablePlugins(GitVersioning)

dockerRepository := Some("jookershop")

dockerExposedPorts := Seq(9000)

packageName in Docker := packageName.value

version in Docker := version.value