package tech.robins
import com.typesafe.config.{Config, ConfigFactory}

trait SimulationApplication {
  protected def getConfig(roles: Array[String], akkaClusterPort: Int = 0): Config = {
    val rolesList = roles.map(role => "\"" + role + "\"").mkString(",")
    ConfigFactory
      .parseString(s"""
           |akka.remote.netty.tcp.port=$akkaClusterPort
           |akka.cluster.roles=[$rolesList]""".stripMargin)
      .withFallback(ConfigFactory.load())
  }
}
