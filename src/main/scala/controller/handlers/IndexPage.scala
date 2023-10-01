package controller.handlers

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}

import scala.io.Source

class IndexPage {

  /**
     Главная страница
   */

  def indexPageHandler(): HttpEntity.Strict = {
    val indexHtmlStream = getClass.getResourceAsStream ("/templates/index.html")
    val indexHtmlContent = Source.fromInputStream (indexHtmlStream).mkString
    HttpEntity (ContentTypes.`text/html(UTF-8)`, indexHtmlContent)
  }
}