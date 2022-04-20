package controllers.admin

import java.util.Date

import models._
import utils.silhouette._
import utils.silhouette.Implicits._
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.util.Credentials
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.impl.exceptions.{ IdentityNotFoundException, InvalidPasswordException }
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import mailer.MailService
import play.api.i18n.{ Messages, MessagesApi }
import utils.admin.Mailer

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import javax.inject.{ Inject, Singleton }

import com.mohiva.play.silhouette.password.BCryptPasswordHasher
import views.html.admin.{ auth => viewsAuth }
import models.admin.MailTokenManager
import utils.silhouette.admin.{ AdminController, ManagerService }

class AuthCtrlxxx @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi, val mailService: MailService) extends AdminController {

  // UTILITIES

  implicit val ms = mailService
  val passwordValidation = nonEmptyText(minLength = 6)

  def notFoundDefault(implicit request: RequestHeader) = Future.successful(NotFound(views.html.admin.errors.notFound(request)))

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // SIGN IN

  val signInForm = Form(tuple(
    "identifier" -> email,
    "password" -> nonEmptyText,
    "rememberMe" -> boolean
  ))

  /**
   * Starts the sign in mechanism. It shows the login form.
   */
  def signIn = UserAwareAction.async { implicit request =>
    Future.successful(request.identity match {
      case Some(_) => Redirect(routes.ApplicationCtrl.index)
      case None => Ok(viewsAuth.signIn(signInForm))
    })
  }

  /**
   * Authenticates the manager based on his email and password
   */
  //  def authenticate = Action.async { implicit request =>
  //    signInForm.bindFromRequest.fold(
  //      formWithErrors => Future.successful(BadRequest(viewsAuth.signIn(formWithErrors))),
  //      formData => {
  //        val (identifier, password, rememberMe) = formData
  //        env.credentialsProvider.authenticate(Credentials(identifier, password)).flatMap { loginInfo =>
  //          env.identityService.retrieve(loginInfo).flatMap {
  //            case Some(manager) => for {
  //              authenticator <- env.authenticatorService.create(loginInfo).map(env.authenticatorWithRememberMe(_, rememberMe))
  //              cookie <- env.authenticatorService.init(authenticator)
  //              result <- env.authenticatorService.embed(cookie, Redirect(routes.ApplicationCtrl.index()))
  //            } yield {
  //              //Log the action in the Audit Trail
  //              implicit val cCookie = coreCookie
  //              env.audit.log { item =>
  //                env.publish(LoginEvent(manager, request, request2Messages))
  //                AuditResponseDto(Some(manager), s"${manager.fullName()} logged-in successfully", s"${manager.fullName()} logged-in successfully on ${new Date().formatted("yyyy-MMM-dd hh:mm")} on a ${cCookie.get.deviceName} ${cCookie.get.deviceType} / ${cCookie.get.deviceCat}", "private_users", manager.id.toString, getModule(s"$ctrlName.authenticate").orNull)
  //              }
  //              result
  //            }
  //            case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
  //          }
  //        }.recover {
  //          case e: ProviderException => Redirect(routes.AuthCtrl.signIn()).flashing("error" -> Messages("auth.credentials.incorrect"))
  //        }
  //      }
  //    )
  //  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // SIGN OUT

  /**
   * Signs out the manager
   */
  //  def signOut = SecuredAction.async { implicit request =>
  //    env.publish(LogoutEvent(request.identity, request, request2Messages))
  //    env.authenticatorService.discard(request.authenticator, Redirect(routes.ApplicationCtrl.index))
  //  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // FORGOT PASSWORD

  val emailForm = Form(single("email" -> email))

  /**
   * Starts the reset password mechanism if the manager has forgot his password. It shows a form to insert his email address.
   */
  def forgotPassword = UserAwareAction.async { implicit request =>
    Future.successful(request.identity match {
      case Some(_) => Redirect(routes.ApplicationCtrl.index)
      case None => Ok(viewsAuth.forgotPassword(emailForm))
    })
  }

  /**
   * Sends an email to the manager with a link to reset the password
   */
  //  def handleForgotPassword = Action.async { implicit request =>
  //    emailForm.bindFromRequest.fold(
  //      formWithErrors => Future.successful(BadRequest(viewsAuth.forgotPassword(formWithErrors))),
  //      email => env.identityService.retrieve(email).flatMap {
  //        case Some(_) => Future.successful {
  //          val token = crypto.Hash.generateSalt
  //          val user = PrivateUsers.find.where().eq("email", email).findUnique()
  //          user.privateToken = token
  //          user.update()
  //          Mailer.activateAccount(email, link = routes.AuthCtrl.resetPassword(token).absoluteURL().replaceAll("https", "http"))
  //          Ok(viewsAuth.forgotPasswordSent(email))
  //        }
  //        case None => Future.successful(BadRequest(viewsAuth.forgotPassword(emailForm.withError("email", Messages("b2b.auth.manager.notexists")))))
  //      }
  //    )
  //  }

  val resetPasswordForm = Form(tuple(
    "password1" -> passwordValidation,
    "password2" -> nonEmptyText
  ) verifying (Messages("auth.passwords.notequal"), passwords => passwords._2 == passwords._1))

  /**
   * Confirms the manager's link based on the token and shows him a form to reset the password
   */
  def resetPassword(tokenId: String) = Action.async { implicit request =>
    PrivateUsers.find.where().eq("private_token", tokenId).findUnique() match {
      case user: PrivateUsers => Future.successful(Ok(viewsAuth.resetPassword(tokenId, resetPasswordForm)))
      case _ => notFoundDefault
    }
  }

  /**
   * Saves the new password and authenticates the manager
   */
  def handleResetPassword(tokenId: String) = UserAwareAction.async { implicit request =>
    resetPasswordForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(viewsAuth.resetPassword(tokenId, formWithErrors))),
      passwords => {
        PrivateUsers.find.where().eq("private_token", tokenId).findUnique() match {
          case user: PrivateUsers => Future.successful {
            val hashedPassword = new BCryptPasswordHasher().hash(passwords._2).password
            //            user.password = hashedPassword
            //            user.privateToken = ""
            user.update()
            if (request.secure)
              Redirect(routes.UserCtrl.privateUsers()).withNewSession.flashing(("success", "Password Reset Successfully. Login to continue."))
            else
              Redirect(routes.AuthCtrl.signIn()).withNewSession.flashing(("success", "Password Reset Successfully. Login to continue."))
          }
          case _ => notFoundDefault
        }
      }
    )
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // CHANGE PASSWORD

  val changePasswordForm = Form(tuple(
    "current" -> nonEmptyText,
    "password1" -> passwordValidation,
    "password2" -> nonEmptyText
  ) verifying (Messages("auth.passwords.notequal"), passwords => passwords._3 == passwords._2))

  /**
   * Starts the change password mechanism. It shows a form to insert his current password and the new one.
   */
  def changePassword = SecuredAction.async { implicit request =>
    Future.successful(Ok(viewsAuth.changePassword(changePasswordForm)))
  }

  /**
   * Saves the new password and renew the cookie
   */
  //  def handleChangePassword = SecuredAction.async { implicit request =>
  //    changePasswordForm.bindFromRequest.fold(
  //      formWithErrors => Future.successful(BadRequest(viewsAuth.changePassword(formWithErrors))),
  //      passwords => {
  //        env.credentialsProvider.authenticate(Credentials(request.identity.email, passwords._1)).flatMap { loginInfo =>
  //          for {
  //            _ <- env.authInfoRepository.update(loginInfo, env.authInfo(passwords._2))
  //            authenticator <- env.authenticatorService.create(loginInfo)
  //            result <- env.authenticatorService.renew(authenticator, Redirect(routes.ApplicationCtrl.myAccount).flashing("success" -> Messages("auth.password.changed")))
  //          } yield result
  //        }.recover {
  //          case e: ProviderException => BadRequest(viewsAuth.changePassword(changePasswordForm.withError("current", Messages("auth.currentpwd.incorrect"))))
  //        }
  //      }
  //    )
  //  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // ACCESS DENIED

  /**
   * Shows an error page when the manager tries to get to an area without the necessary roles.
   */
  def accessDenied = SecuredAction.async { implicit request =>
    Future.successful(Ok(viewsAuth.accessDenied()))
  }

}