package tech.robins
import java.io.File

import com.typesafe.config.ConfigFactory
import org.kohsuke.github.GitHub

object GitHubApi {
  private val configFilePath = getClass.getClassLoader.getResource("gitHubAccount.conf").getPath
  private val gitHubAccountConfigFile = new File(configFilePath)
  private val config = ConfigFactory.parseFile(gitHubAccountConfigFile)

  val username: String = config.getString("account.name")
  val token: String = config.getString("account.oAuthToken")

  val authenticatedApi: GitHub = GitHub.connect(username, token)
}
