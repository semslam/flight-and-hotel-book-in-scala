package controllers.admin.management

import java.util.UUID

import javax.inject.{ Inject, Singleton }
import com.avaje.ebean.Ebean
import com.google.common.net.HttpHeaders
import com.mohiva.play.silhouette.api.Silhouette
import crypto.Encrypt
import mailer.MailService
import models._
import play.api.i18n.MessagesApi
import utils.admin.Mailer
import utils.silhouette.MyEnv
import utils.silhouette.admin.{ AdminController, WithRoles }
import views.html.admin._

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

/**
 * Created by Igbalajobi Jamiu Okunade on 5/21/17.
 */
@Singleton
class B2BSystemCtrl @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi, implicit val encrypt: Encrypt, mailService: MailService) extends AdminController {

  val roles = WithRoles(Roles.sale_manager.name(), Roles.operation_manager.name(), Roles.agent_manager.name(), Roles.admin.name())
  implicit val ms = mailService

  def index = SecuredAction(roles) { implicit request =>
    val agents = Users.find.where().in("role", Roles.b2b_owner.name(), Roles.b2b_sales_agent.name()).order().desc("id").findList()
    Ok(b2bsystem.index(agents.asScala.toList))
  }

  def detail(id: String) = SecuredAction(roles) { implicit request =>
    Ok(b2bsystem.detailModal(Users.find.byId(java.lang.Long.valueOf(encrypt.decrypt(id)))))
  }

  def blockCustomer(uid: String, status: String, uCat: String) = SecuredAction(roles) { implicit request =>
    val u = Users.find.byId(java.lang.Long.valueOf(encrypt.decrypt(uid)))
    u.setStatus(models.Status.valueOf(status))
    val uuid = UUID.randomUUID().toString
    u.setEmail(u.getEmail + uuid)
    u.setPhone(u.getPhone + uuid)
    u.getAgentCorporateDetailId.setCompanyName(u.getAgentCorporateDetailId.getCompanyName + uuid)
    u.getAgentCorporateDetailId.update()
    u.update()
    u.getAgentCorporateDetailId.delete()
    u.delete()
    Redirect(routes.B2BSystemCtrl.index()).withNewSession.flashing(("success", "User Disabled Successfully."))
  }

  def moderateAgent(id: String) = SecuredAction(WithRoles(Roles.admin.name(), Roles.agent_manager.name())) { implicit request =>
    Ebean.beginTransaction()
    try {
      val u = Users.find.byId(java.lang.Long.valueOf(encrypt.decrypt(id)))
      u.setStatus(models.Status.Active)
      u.update()
      //B2B User Agent
      val markUp = new B2bUserMarkUp
      markUp.setUserId(u)
      markUp.insert()
      val conf = play.Configuration.root()
      Mailer.b2bAgentApprove(u, link = controllers.admin.routes.AuthCtrl.resetPassword(u.getActivationToken).absoluteURL().replaceAllLiterally(conf.getString("project.subdomain.admin"), conf.getString("project.subdomain.b2b")).replaceAll("http", "https"))
      Ebean.commitTransaction()
      Redirect(controllers.admin.management.routes.B2BSystemCtrl.index).withNewSession.flashing(("success", "Account Updated. Successfully. Email Sent to User to Update Password."))
    } catch {
      case e: Throwable =>
        Ebean.rollbackTransaction()
        Redirect(controllers.admin.management.routes.B2BSystemCtrl.index).withNewSession.flashing(("danger", "Error in Exception! Account could not be activated. Please contact technical support."))
    }
  }

}
