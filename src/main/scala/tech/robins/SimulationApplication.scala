package tech.robins
import com.typesafe.config.{Config, ConfigFactory}

object SimulationApplication {
  def getConfig(roles: Array[String], akkaClusterPort: Int = 0): Config = {
    val rolesList = roles.map(role => "\"" + role + "\"").mkString(",")
    ConfigFactory
      .parseString(s"""
           |akka.remote.netty.tcp.port=$akkaClusterPort
           |akka.cluster.roles=[$rolesList]""".stripMargin)
      .withFallback(ConfigFactory.load())
  }

  def main(args: Array[String]): Unit = args.headOption match {
    case Some("master") => SimulationApplicationMaster.runMain()
    case Some("worker") => SimulationApplicationWorker.runMain(args.tail)
    case _              => throw new IllegalArgumentException("First argument must be 'master' or 'worker'")
  }
}
