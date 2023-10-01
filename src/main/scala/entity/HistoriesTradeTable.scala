package entity

import slick.jdbc.PostgresProfile.api._
import slick.lifted

import java.sql.Date

class HistoriesTradeTable(tag: Tag) extends Table[HistoryTrade](tag, "histories_trade") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def secid = column[String]("secid")
  def tradedate = column[Option[Date]]("tradedate", O.Default(None))
  def numtrades = column[String]("numtrades")
  def open = column[Double]("open")
  def close = column[Double]("close")

  def * = (id.?, secid, tradedate, numtrades, open, close) <> (HistoryTrade.tupled, HistoryTrade.unapply)
  def secidFK = foreignKey("secid_fk", secid, lifted.TableQuery[SecuritiesHubTable])(_.secid)
}
