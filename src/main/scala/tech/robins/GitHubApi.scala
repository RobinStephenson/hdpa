package tech.robins

import com.typesafe.config.ConfigFactory
import org.kohsuke.github.GitHub

object GitHubApi {
  private val config = ConfigFactory.load(getClass.getClassLoader, "gitHubAccount.conf")

  val username: String = config.getString("account.name")
  val token: String = config.getString("account.oAuthToken")

  val authenticatedApi: GitHub = GitHub.connect(username, token)
}
