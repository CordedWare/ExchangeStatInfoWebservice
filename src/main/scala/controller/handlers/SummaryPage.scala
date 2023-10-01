package controller.handlers

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import config.AppServer.{db, historiesTradeTable, securitiesHubTable}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class SummaryPage {

  /**
   * Сводные данные
   */

  def summaryPageHandler(): HttpEntity.Strict = {
    val historiesTrade = Await.result(db.run(historiesTradeTable.result), 5.seconds)
    val securitiesHub = Await.result(db.run(securitiesHubTable.result), 5.seconds)
    val historyTable = historiesTrade.map { trade =>
      securitiesHub.find(_.secid == trade.secid).map { security =>
        s"""
              <tr>
                <td>${security.secid}</td>
                <td>${security.regnumber}</td>
                <td>${security.name}</td>
                <td>${security.emitentTitle}</td>
                <td>${trade.tradedate.getOrElse("")}</td>
                <td>${trade.numtrades}</td>
                <td>${trade.open}</td>
                <td>${trade.close}</td>
              </tr>
            <br>"""
      }
    }.mkString("\n")
    val responseHtml = historyTable.replace("{history_table}", historyTable)
    HttpEntity(ContentTypes.`text/html(UTF-8)`, responseHtml)
  }

}
