name := "oap-perf-suite"

version := "1.0"

scalaVersion := "2.11.12"

unmanagedJars in Compile += file("/home/oap/oap_jars/oap.jar")
unmanagedJars in Compile += file("/home/oap/oap_jars/spark-sql-perf_2.11-0.4.11-SNAPSHOT.jar")

libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.3.0" % "provided"
libraryDependencies += "org.reflections" % "reflections" % "0.9.10" % "compile"
