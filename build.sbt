seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

name := "open-street-map-importer"
 
version := "1.0.0"

artifactName in com.github.retronym.SbtOneJar.oneJar := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
  artifact.name + "-" + module.revision + "." + artifact.extension
}
 
scalaVersion := "2.10.2"
 
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "spray repo"          at "http://repo.spray.io"
 
libraryDependencies += "com.typesafe.akka"         %% "akka-actor"     % "2.1.4"

libraryDependencies += "io.spray"                  % "spray-can"       % "1.1-M8"

libraryDependencies += "io.spray"                  % "spray-caching"   % "1.1-M8"

libraryDependencies += "io.spray"                  % "spray-routing"   % "1.1-M8"

libraryDependencies += "org.apache.httpcomponents" % "fluent-hc"       % "4.2.5"

libraryDependencies += "ch.qos.logback"            % "logback-classic" % "1.0.13"

libraryDependencies += "com.typesafe"              % "config"          % "1.0.2"

libraryDependencies += "org.scalaz"                %% "scalaz-core"    % "7.0.2"

libraryDependencies += "com.typesafe.slick"        %% "slick"          % "1.0.1"

libraryDependencies += "mysql"                     % "mysql-connector-java" % "5.1.25"


