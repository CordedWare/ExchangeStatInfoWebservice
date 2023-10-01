package entity

import java.sql.Date

case class HistoryTrade(id: Option[Long],
                        secid: String,
                        tradedate: Option[Date],
                        numtrades: String,
                        open: Double,
                        close: Double)
