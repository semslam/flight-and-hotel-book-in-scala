package controllers.admin.reports

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import models.{ AuditLog, PrivateUsers, Roles }
import play.api.i18n.MessagesApi

import utils.silhouette.admin.{ AdminController, WithRoles }
import utils.silhouette.MyEnv

/**
 * Created by Igbalajobi Jamiu O. on 29/09/2016 3:16 PM.
 */
class AuditCtrl @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi) extends AdminController {

  val roles = WithRoles(Roles.super_admin.name())

  def index = SecuredAction(roles) { implicit request =>
    import scala.collection.JavaConverters._
    Ok(views.html.admin.audits.index(AuditLog.find.all().asScala.toList))
  }
}
