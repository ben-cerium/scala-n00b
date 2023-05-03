name := "HelloWorld"
version := "0.1"
scalaVersion := "3.2.2"

libraryDependencies += "io.cucumber" %% "cucumber-scala" % "8.14.2" % Test
libraryDependencies += "io.cucumber" % "cucumber-junit" % "7.11.2" % Test
libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test
libraryDependencies += "software.amazon.awssdk" % "s3" % "2.20.20"
libraryDependencies += "software.amazon.awssdk" % "sns" % "2.20.20"
libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "2.0.7",
  "org.slf4j" % "slf4j-simple" % "2.0.7"
)
libraryDependencies += "com.jayway.jsonpath" % "json-path" % "2.8.0"
