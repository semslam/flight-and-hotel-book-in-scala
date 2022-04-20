package models.api

import org.joda.time.DateTime
import java.util.UUID

import models.ApiToken

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/*
* Stores the Auth Token information. Each token belongs to a Api Key and user
*/

object ApiToken {
  import ApiModel.tokens

  def findByTokenAndApiKey(token: String, apiKey: String): Future[Option[ApiToken]] = Future.successful {
    Option(models.ApiToken.find.where().eq("token", token).where().eq("apiKey", apiKey).findUnique())
  }

  def create(apiKey: String, userId: Long): Future[String] = Future.successful {
    // Be sure the uuid is not already taken for another token
    val token = UUID.randomUUID().toString
    val apiToken = new models.ApiToken
    apiToken.setApiKey(apiKey)
    apiToken.setToken(token)
    apiToken.save()
    token
    //    def newUUID: String = {
    //      val uuid =
    //      if (!tokens.exists(_.token == uuid)) uuid else newUUID
    //    }
    //    val token = newUUID
    //    tokens.insert(_ => ApiToken(token, apiKey, expirationTime = (new DateTime()) plusMinutes 10, userId))
    //    token
  }

  def delete(token: String): Future[Unit] = Future.successful {
    tokens.where().eq("token", token).delete()
  }
}
