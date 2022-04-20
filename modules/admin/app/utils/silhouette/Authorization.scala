package utils.silhouette.admin

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models.{ PrivateUsers, Roles }
import play.api.mvc.Request
import play.api.i18n.Messages

import scala.concurrent.Future

/**
 * Only allows those managers that have at least a role of the selected.
 * Master role is always allowed.
 * Ex: WithRole("high", "sales") => only managers with roles "high" OR "sales" (or "master") are allowed.
 */
case class WithRole(anyOf: String*) extends Authorization[PrivateUsers, CookieAuthenticator] {
  def isAuthorized[A](manager: PrivateUsers, authenticator: CookieAuthenticator)(implicit r: Request[A]) = Future.successful {
    WithRole.isAuthorized(manager, anyOf: _*)
  }
}
object WithRole {
  def isAuthorized(manager: PrivateUsers, anyOf: String*): Boolean =
    anyOf.intersect(manager.getuRoles.split("<<>>")).nonEmpty || manager.getuRoles.split("<<>>").contains(Roles.super_admin.name())
}

/**
 * Only allows those managers that have every of the selected roles.
 * Master role is always allowed.
 * Ex: Restrict("high", "sales") => only managers with roles "high" AND "sales" (or "master") are allowed.
 */
case class WithRoles(allOf: String*) extends Authorization[PrivateUsers, CookieAuthenticator] {
  def isAuthorized[A](manager: PrivateUsers, authenticator: CookieAuthenticator)(implicit r: Request[A]) = Future.successful {
    WithRoles.isAuthorized(manager, allOf: _*)
  }
}

object WithRoles {
  def isAuthorized(manager: PrivateUsers, allOf: String*): Boolean =
    allOf.intersect(manager.getuRoles.split("<<>>")).length == allOf.length || models.Manager.userRoles(manager).contains(Roles.super_admin.name())
}