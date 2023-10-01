package entity

import slick.jdbc.PostgresProfile.api._

class SecuritiesHubTable(tag: Tag) extends Table[SecurityHub](tag, "securities_hub") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def secid = column[String]("secid")
  def regnumber = column[String]("reg_number")
  def name = column[String]("short_name")
  def emitentTitle = column[String]("emitent_title")

  def * = (id.?, secid, regnumber, name, emitentTitle) <> (SecurityHub.tupled, SecurityHub.unapply)
}