package tech.robins.execution
import akka.actor.Props

object ExecutionNodeLibrary {
  // Name -> Builder
  val builders: Map[String, ExecutionNodeBuilder] = Map(
    "AlwaysAccepting" -> AlwaysAcceptingExecutionNode,
    "OneRejection" -> OneRejectionExecutionNode
  )
}
