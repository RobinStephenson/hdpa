package tech.robins
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import tech.robins.SimulationController.StartSimulation

import scala.io.StdIn

object SimulationApplicationMaster extends SimulationApplication {
  val roleName = "simulationMaster"

  def main(args: Array[String]): Unit = {
    val roles = Array(roleName)
    val masterPort = 2551
    val configWithPort = getConfig(roles, masterPort)
    val system = ActorSystem("SimulationSystem", configWithPort)
    val materializer = ActorMaterializer()(system)
    val simConfig: SimulationConfiguration = SimulationConfiguration(configWithPort)
    val simController = system.actorOf(SimulationController.props(simConfig, materializer), "simController")

    println("Press ENTER to start the simulation")
    try StdIn.readLine()
    finally {
      println("----  ----  STARTING SIMULATION  ----  ----")
      simController ! StartSimulation
    }
  }
}
