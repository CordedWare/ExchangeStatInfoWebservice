package config

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.ByteString
import controller.handlers.{IndexPage, SummaryPage, UniversalXMLHandler}
import _root_.entity.{HistoriesTradeTable, SecuritiesHubTable, SecurityHub}
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}
import scala.xml.XML
import slick.jdbc.PostgresProfile.api._

import java.util.concurrent.Executors


object AppServer extends App {

  /**
   * Конфигуратор с настройками контроллеров
   */

  implicit val system: ActorSystem = ActorSystem("ExchangeStatInfoWebservice")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val dbConfig = DatabaseConfig.loadConfig()

  val db = Database.forURL(dbConfig.dbUrl, dbConfig.dbUser, dbConfig.dbPassword)

  val executor: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))
  implicit val session: Session = db.createSession()

  val securitiesHubTable = TableQuery[SecuritiesHubTable]
  val createSecTableAction = securitiesHubTable.schema.createIfNotExists
  val setupSecFuture = db.run(createSecTableAction)

  val historiesTradeTable = TableQuery[HistoriesTradeTable]
  val createHisTableAction = historiesTradeTable.schema.createIfNotExists
  val setupHisFuture = db.run(createHisTableAction)

  // TODO доработать пулы соединений
  val routes =
    pathSingleSlash {
      redirect("/index", StatusCodes.PermanentRedirect)
    } ~
      path("") {
        get {
          getFromResource("resources/templates/index.html")
        }
      } ~
      pathPrefix("images") {
        get {
          getFromResourceDirectory("static/images")
        }
      } ~
      path("index") {
        get {
          val indexPage = new IndexPage()
          complete(indexPage.indexPageHandler())
        }
      } ~
      path("insertSecurities") {
        post {
          formFields("secid", "regnumber", "name", "emitentTitle") { (secid, regnumber, name, emitentTitle) =>
            val securitie = SecurityHub(None, secid, regnumber, name, emitentTitle)
            val insertAction = securitiesHubTable returning securitiesHubTable.map(_.id) into ((securitie, id) => securitie.copy(id = Some(id))) += securitie
            val future = db.run(insertAction)

            onSuccess(future) { insertedSecuritie =>
              complete(s"Сохранено с ID ${insertedSecuritie.id}")
            }
          }
        }
      } ~
      path("summary") {
        {
          val summaryPage = new SummaryPage()
          complete(summaryPage.summaryPageHandler())
        }
      } ~
      path("upload") {
        post {
          fileUpload("file") { case (metadata, file) =>
            val xmlString = file.map(_.utf8String).runFold("")(_ + _)
            val parsedData = XML.loadString(Await.result(xmlString, 5.seconds))
            val rows = (parsedData \\ "row")
            val parse = new UniversalXMLHandler()
            parse.xmlParsing(rows)

            complete("Данные получены и успешно сохранены!")
          }
        }
      } ~
      path("getApiHistories") {
        get {
          val totalSize = 585
          var start = 0
          val step = 100
          var isLoadSuccess = true
          var isFinalSuccess = true

          def processXML(xml: String): Unit = {
            val rows = scala.xml.XML.loadString(xml) \\ "row"
            val parse = new UniversalXMLHandler()
            parse.xmlParsing(rows)
          }

          def makeRequest(): Future[Unit] = {
            val responseFuture = Http().singleRequest(HttpRequest(uri = "https://iss.moex.com/iss/history/engines/stock/markets/shares/securities/?start=" + start))
            responseFuture.flatMap(response => response.entity.dataBytes.runFold(ByteString.empty)(_ ++ _))
              .map(_.utf8String)
              .map(xml => processXML(xml))
          }

          def processRequests(): Future[Unit] = {
            val requestFuture = makeRequest()
            requestFuture.flatMap(_ => {
              if (start >= totalSize) {
                isLoadSuccess = false
                isFinalSuccess = true
                Future.successful(())
              } else {
                start += step
                processRequests()
              }
            })
          }

          val finalFuture = processRequests()
          Await.result(finalFuture, Duration.Inf)

          complete(StatusCodes.OK)
        }
       } ~
      path("deleteAll") {
    post {
      val deleteAction = for {
        _ <- historiesTradeTable.delete
        _ <- securitiesHubTable.delete
      } yield ()
      val deleteFuture = db.run(deleteAction)

      onComplete(deleteFuture) {
        case Success(_) => complete("Удаление успешно завершено!")
        case Failure(ex) => complete(s"Удаление с ошибкой: ${ex.getMessage}")
      }
    }
  }

  Http().bindAndHandle(routes, "localhost", 8080)
  system.registerOnTermination(() => db.close())

}