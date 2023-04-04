name := "HelloWorld"
version := "0.1"
scalaVersion := "3.2.2"

libraryDependencies += "io.cucumber" % "cucumber-scala" % "8.14.2" % Test
libraryDependencies += "io.cucumber" % "cucumber-junit" % "7.11.2" % Test
libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test
libraryDependencies += "software.amazon.awssdk" % "s3" % "2.20.20"
