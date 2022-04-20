package utils.silhouette.b2b

import models.{ User, Users }
import utils.silhouette.Implicits._
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService

import scala.concurrent.Future

class B2BService extends IdentityService[Users] {
  def retrieve(loginInfo: LoginInfo): Future[Option[Users]] = User.findAgentByEmail(loginInfo)

  def save(manager: Users): Future[Users] = User.save(manager)
}