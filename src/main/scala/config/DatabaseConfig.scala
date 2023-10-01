package config

import com.typesafe.config.{Config, ConfigFactory}
import slick.jdbc.PostgresProfile.api._
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

class DatabaseConfig(config: Config) {

  /**
   * Конфиг базы данных и настройка пулов соединений.
   */

  val dbUrl: String = config.getString("dbUrl")
  val dbUser: String = config.getString("dbUser")
  val dbPassword: String = config.getString("dbPassword")
  val executor: String = config.getString("executor")
  val jndiName: String = config.getString("jndiName")
  val numThreads: Int = config.getInt("numThreads")
  val queueSize: Int = config.getInt("queueSize")

  def createDatabase: Database = {
    val asyncExecutor = AsyncExecutor(
      name = executor,
      numThreads = numThreads,
      queueSize = queueSize
    )
    Database.forURL(dbUrl, dbUser, dbPassword, driver = "org.postgresql.Driver", executor = asyncExecutor)
  }


  def createExecutionContext: ExecutionContext = {
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(numThreads.toInt))
  }
}

object DatabaseConfig {
  def loadConfig(): DatabaseConfig = {
    val appConfig =  ConfigFactory.load()
    val dbConfig = appConfig.getConfig("db")
    new DatabaseConfig(dbConfig)
  }
}
