package models.api

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object ApiKey {

  import ApiModel.apiKeys

  def isActive(apiKey: String): Future[Option[Boolean]] = Future.successful {
    Option(apiKeys.where().eq("apiKey", apiKey).findRowCount() > 0)
  }

}
