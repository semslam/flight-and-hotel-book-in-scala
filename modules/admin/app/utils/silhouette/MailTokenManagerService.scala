package utils.silhouette.admin

import models.admin.MailTokenManager
import utils.silhouette.MailTokenService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class MailTokenManagerService extends MailTokenService[MailTokenManager] {
  def create(token: MailTokenManager): Future[Option[MailTokenManager]] = {
    MailTokenManager.save(token).map(Some(_))
  }
  def retrieve(id: String): Future[Option[MailTokenManager]] = {
    MailTokenManager.findById(id)
  }
  def consume(id: String): Unit = {
    MailTokenManager.delete(id)
  }
}