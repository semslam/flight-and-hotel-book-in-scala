package controllers.admin

import javax.inject.Inject

import aws.AwsS3Services
import com.avaje.ebean.{ Ebean, TxRunnable }
import com.google.common.net.HttpHeaders
import com.mohiva.play.silhouette.api.{ LoginInfo, Silhouette }
import com.mohiva.play.silhouette.password.BCryptPasswordHasher
import crypto.{ Encrypt, Hash }
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.i18n.{ Messages, MessagesApi }
import play.api.mvc.Action
import mailer.MailService
import utils.admin.Mailer
import utils.silhouette.MyEnv

import scala.concurrent.Future
import views.html.admin._
import models.{ Status, _ }
import utils.admin.Mailer

import scala.collection.JavaConverters._
import scala.util.Random
import models.admin.MailTokenManager
import utils.silhouette.admin.{ AdminController, ManagerService, WithRoles }

/**
 * Created by
 * Igbalajobi Jamiu O. on 26/05/2016 6:51 AM.
 */
case class UserPrivateDAO(id: Option[Long], firstName: String, lastName: String, phone: String, role: List[String], email: String)

class UserCtrl @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi, encrypt: Encrypt, s3Client: AwsS3Services, val mailService: MailService) extends AdminController {

  val role = WithRoles(Roles.admin.name())

  var avatar = "vectatravelavatar"

  implicit val enc: Encrypt = this.encrypt
  implicit val ms = mailService

  val privateDAOForm = Form(mapping(
    "id" -> optional(longNumber),
    "firstName" -> nonEmptyText,
    "lastName" -> nonEmptyText,
    "phone" -> text.verifying(minLength(11)),
    "role" -> list(text),
    "email" -> email.verifying("Ooops! Email already exists", email => PrivateUsers.find.where().eq("email", email).findRowCount() == 0)
  )(UserPrivateDAO.apply)(UserPrivateDAO.unapply))

  def customers = SecuredAction(WithRoles(Roles.admin.name, Roles.agent_manager.name())) { implicit request =>
    Ok(user.usersList(Users.find.where().eq("role", Roles.b2c_customer.name()).findList().asScala.toList))
  }

  def deletePrivate(uid: String) = SecuredAction(role) { implicit request =>
    val u = PrivateUsers.find.byId(java.lang.Long.valueOf(encrypt.decrypt(uid)))
    u.setEmail(null);
    u.update()
    u.delete()
    Redirect(request.headers.get(HttpHeaders.FROM).getOrElse("/")).withNewSession.flashing(("success", s"${u.fullName()} Account Deleted Successfully."))
  }

  val resetPasswordForm = Form(tuple(
    "password1" -> nonEmptyText(minLength = 6),
    "password2" -> nonEmptyText
  ) verifying (Messages("auth.passwords.notequal"), passwords => passwords._2 == passwords._1))

  def changePassword(uid: String) = SecuredAction(role) { implicit request =>
    val privateToken = Hash.generateSalt()
    val u = PrivateUsers.find.byId(java.lang.Long.valueOf(encrypt.decrypt(uid)))
    u.setPrivateToken(privateToken)
    u.update()
    Ok(user.changePassword(privateToken, resetPasswordForm, u))
  }

  def resetPassword(activationToken: String) = Action(Ok("")) //Note: Don't delete. For absolute URL only.

  def privateUsers = SecuredAction(role) { implicit request =>
    Ok(user.privateUsers(PrivateUsers.find.all().asScala.toList))
  }

  def createPrivate(id: String) = SecuredAction(role) { implicit request =>
    if (id != null && id != "") {
      val privateUser = PrivateUsers.find.byId(encrypt.decrypt(id).toLong)
      Ok(user.createPrivate(privateDAOForm.fill(UserPrivateDAO(
        id = Some(privateUser.getId),
        firstName = privateUser.getFirstName,
        lastName = privateUser.getLastName,
        phone = privateUser.getPhone,
        email = privateUser.getEmail,
        role = privateUser.getuRoles().split("<<>>").toList
      ))))
    } else Ok(user.createPrivate(privateDAOForm))
  }

  def savePrivate = SecuredAction(role).async { implicit request =>
    Future.successful {
      privateDAOForm.bindFromRequest().fold(
        formWithError => {
          BadRequest(user.createPrivate(formWithError))
        },
        formInputValue => {
          val staffRef = "STF" + Random.nextInt(4)
          val privateUser = new PrivateUsers
          privateUser.setFirstName(formInputValue.firstName)
          privateUser.setLastName(formInputValue.lastName)
          privateUser.setEmail(formInputValue.email)
          privateUser.setPhone(formInputValue.phone)
          privateUser.setStatus(models.Status.Inactive)
          privateUser.setStaffRef(staffRef)
          val strBuilder = new StringBuilder
          formInputValue.role.foreach(a => strBuilder.append(s"$a<<>>"))
          privateUser.setuRoles(strBuilder.toString)
          privateUser.save()
          Mailer.createStaff(privateUser, link = routes.AuthCtrl.forgotPassword(formInputValue.email).absoluteURL())
          Redirect(routes.UserCtrl.privateUsers).withNewSession.flashing(("success", "User Created Successfully"))
        }
      )
    }
  }
}
