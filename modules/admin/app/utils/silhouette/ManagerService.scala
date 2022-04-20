package utils.silhouette.admin

import models.{ Manager, PrivateUsers }
import utils.silhouette.Implicits._
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService

import scala.concurrent.Future

//TODO -> Manager Serice
class ManagerService extends IdentityService[PrivateUsers] {
  def retrieve(loginInfo: LoginInfo): Future[Option[PrivateUsers]] = Manager.findByEmail(loginInfo.providerKey)
  def save(manager: PrivateUsers): Future[PrivateUsers] = Manager.save(manager)
}