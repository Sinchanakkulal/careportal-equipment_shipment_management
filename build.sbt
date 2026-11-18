name := """carePortal-Equipment&Shipment_management"""
organization := "Agile"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.13.18"

libraryDependencies ++= Seq(
    guice,
    javaJdbc,
    "com.mysql" % "mysql-connector-j" % "26.7.0"
)