name := "oap-perf-suite"

version := "1.0"

scalaVersion := "2.11.12"

unmanagedJars in Compile += file("/home/oap/DailyTest/OAP/target/oap-0.5.0-SNAPSHOT-with-spark-2.3.0.jar")
unmanagedJars in Compile += file("/home/oap/oap_jars/spark-sql-perf_2.11-0.4.11-SNAPSHOT.jar")

libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.3.0" % "provided"
libraryDependencies += "org.reflections" % "reflections" % "0.9.10" % "compile"
