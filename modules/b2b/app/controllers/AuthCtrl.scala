package controllers.b2b

import crypto.Encrypt
import models._
import utils.silhouette._
import com.mohiva.play.silhouette.password._
import utils.silhouette.Implicits._
import com.mohiva.play.silhouette.api.{ LoginEvent, LoginInfo, LogoutEvent, SignUpEvent, Silhouette }
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.{ Clock, Credentials, PasswordHasherRegistry }
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.exceptions.{ IdentityNotFoundException, InvalidPasswordException }
import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import play.api._
import play.api.mvc._
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.i18n.{ Messages, MessagesApi }
import utils.b2b.Mailer

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import net.ceedubs.ficus.Ficus._
import javax.inject.{ Inject, Singleton }

import akka.actor.Props
import api.ServicesAPI
import com.avaje.ebean.annotation.Transactional
import com.google.inject.name.Named
import mailer.MailService
import views.html.b2b.{ auth => viewsAuth }
import utils.b2b.Mailer
import utils.silhouette.b2b.B2BController
import utils.silhouette.b2b.B2BService
import controllers.b2b.routes
import scala.collection.JavaConversions._

@Singleton
class AuthCtrl @Inject() (
    val silhouette: Silhouette[MyEnv[Users]],
    val messagesApi: MessagesApi,
    b2bService: B2BService,
    userService: UserService,
    @Named("auth-info-repository") authInfoRepository: AuthInfoRepository,
    @Named("credentials-provider") credentialsProvider: CredentialsProvider,
    val mailService: MailService,
    passwordHasherRegistry: PasswordHasherRegistry,
    conf: Configuration,
    servicesAPI: ServicesAPI,
    clock: Clock,
    implicit val encrypt: Encrypt
) extends B2BController {

  // UTILITIES

  implicit val ms = mailService
  val passwordValidation = nonEmptyText(minLength = 6)

  def notFoundDefault(implicit request: RequestHeader) = Future.successful(NotFound(views.html.b2b.errors.notFound(request)))

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // SIGN IN

  val signInForm = Form(tuple(
    "identifier" -> email,
    "password" -> nonEmptyText,
    "rememberMe" -> boolean
  ))

  val signUpForm = Form(mapping(
    "title" -> nonEmptyText,
    "firstName" -> nonEmptyText,
    "lastName" -> nonEmptyText,
    "phone" -> nonEmptyText.verifying(maxLength(11)),
    "companyName" -> nonEmptyText.verifying("Company name already exists", name => AgentCorporateDetails.find.where().eq("company_name", name).findRowCount() == 0),
    "address" -> nonEmptyText,
    "city" -> nonEmptyText,
    "country" -> nonEmptyText,
    "email" -> email.verifying("Ooops! Email already exists", email => Users.find.where().eq("email", email).findRowCount() == 0),
    "websiteUrl" -> optional(nonEmptyText),
    "confirmEmail" -> nonEmptyText,
    "termsCondition" -> boolean
  )(B2BSignUp.apply)(B2BSignUp.unapply).verifying("Email did not match", data => data.email == data.confirmEmail))

  /**
   * Starts the sign in mechanism. It shows the login form.
   */
  def signIn = UserAwareAction { implicit request =>
    request.identity match {
      case Some(_) => Redirect(routes.ApplicationCtrl.index)
      case None => Ok(viewsAuth.signIn(signInForm))
    }
  }

  def signUp = UserAwareAction.async { implicit request =>
    Future.successful(request.identity match {
      case Some(_) => Redirect(routes.ApplicationCtrl.index)
      case _ => Ok(viewsAuth.signUp(signUpForm))
    })
  }

  /**
   * Authenticates the manager based on his email and password
   */
  def authenticate = UnsecuredAction.async { implicit request =>
    signInForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(viewsAuth.signIn(formWithErrors))),
      formData => {
        val (identifier, password, rememberMe) = formData
        credentialsProvider.authenticate(Credentials(identifier, password)).flatMap { loginInfo =>
          b2bService.retrieve(loginInfo).flatMap {
            case Some(manager) => for {
              authenticator <- env.authenticatorService.create(loginInfo).map(authenticatorWithRememberMe(_, rememberMe))
              cookie <- env.authenticatorService.init(authenticator)
              result <- env.authenticatorService.embed(cookie, Redirect(routes.ApplicationCtrl.index))
            } yield {
              env.eventBus.publish(LoginEvent(manager, request))
              result
            }
            case None => Future.failed(new IdentityNotFoundException("Couldn't find manager"))
          }
        }.recover {
          case e: ProviderException => Redirect(routes.AuthCtrl.signIn).flashing("error" -> Messages("auth.credentials.incorrect"))
        }
      }
    )
  }

  private def authenticatorWithRememberMe(authenticator: CookieAuthenticator, rememberMe: Boolean) = {
    if (rememberMe) {
      authenticator.copy(
        expirationDateTime = clock.now + rememberMeParams._1,
        idleTimeout = rememberMeParams._2,
        cookieMaxAge = rememberMeParams._3
      )
    } else
      authenticator
  }

  private lazy val rememberMeParams: (FiniteDuration, Option[FiniteDuration], Option[FiniteDuration]) = {
    val cfg = conf.getConfig("silhouette.authenticator.rememberMe").get.underlying
    (
      cfg.as[FiniteDuration]("authenticatorExpiry"),
      cfg.getAs[FiniteDuration]("authenticatorIdleTimeout"),
      cfg.getAs[FiniteDuration]("cookieMaxAge")
    )
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // SIGN OUT

  /**
   * Signs out the manager
   */
  def signOut = SecuredAction.async { implicit request =>
    env.eventBus.publish(LogoutEvent(request.identity, request))
    env.authenticatorService.discard(request.authenticator, Redirect(routes.ApplicationCtrl.index))
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // FORGOT PASSWORD

  val emailForm = Form(single("email" -> email))

  /**
   * Starts the reset password mechanism if the manager has forgot his password. It shows a form to insert his email address.
   */
  def forgotPassword = UserAwareAction { implicit request =>
    request.identity match {
      case Some(_) => Redirect(routes.ApplicationCtrl.index)
      case None => Ok(viewsAuth.forgotPassword(emailForm))
    }
  }

  /**
   * Sends an email to the manager with a link to reset the password
   */
  def handleForgotPassword = UnsecuredAction.async { implicit request =>
    emailForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(viewsAuth.forgotPassword(formWithErrors))),
      email => env.identityService.retrieve(email).flatMap {
        case Some(_) =>
          val token = MailTokenUser(email, isSignUp = false)
          userService.createTokenMail(token).map { _ =>
            Mailer.forgotPassword(email, link = routes.AuthCtrl.resetPassword(token.id).absoluteURL())
            Ok(viewsAuth.forgotPasswordSent(email))
          }
        case None => Future.successful(BadRequest(viewsAuth.forgotPassword(emailForm.withError("email", Messages("web.auth.user.notexists")))))
      }
    )
  }

  val resetPasswordForm = Form(tuple(
    "password1" -> passwordValidation,
    "password2" -> nonEmptyText
  ) verifying (Messages("auth.passwords.notequal"), passwords => passwords._2 == passwords._1))

  /**
   * Confirms the manager's link based on the token and shows him a form to reset the password
   */
  def resetPassword(tokenId: String) = UnsecuredAction.async { implicit request =>
    userService.activateToken(tokenId).flatMap {
      case Some(token) => Future.successful(Ok(viewsAuth.resetPassword(tokenId, resetPasswordForm)))
      case None => notFoundDefault
    }
  }

  /**
   * Saves the new password and authenticates the manager
   */
  def handleResetPassword(tokenId: String) = UnsecuredAction.async { implicit request =>
    resetPasswordForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(viewsAuth.resetPassword(tokenId, formWithErrors))),
      success = passwords => {
        userService.activateToken(tokenId).flatMap {
          case Some(token) =>
            val loginInfo: LoginInfo = token.getEmail
            env.identityService.retrieve(loginInfo).flatMap {
              case Some(user) => for {
                _ <- userService.resetPassword(user, passwords._2)
                authenticator <- env.authenticatorService.create(LoginInfo(user.getId.toString, user.getEmail)).map(authenticatorWithRememberMe(_, true))
                cookie <- env.authenticatorService.init(authenticator)
                result <- env.authenticatorService.renew(authenticator, Ok(viewsAuth.resetedPassword(user)))
              } yield {
                env.eventBus.publish(LoginEvent(user, request))
                result
              }
              case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
            }
          case None => notFoundDefault
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
  def changePassword = SecuredAction { implicit request =>
    Ok(viewsAuth.changePassword(changePasswordForm))
  }

  /**
   * Saves the new password and renew the cookie
   */
  def handleChangePassword = SecuredAction.async {
    implicit request =>
      changePasswordForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(viewsAuth.changePassword(formWithErrors))),
        passwords => {
          credentialsProvider.authenticate(Credentials(request.identity.getEmail, passwords._1)).flatMap {
            loginInfo =>
              for {
                _ <- authInfoRepository.update(loginInfo, passwordHasherRegistry.current.hash(passwords._2))
                authenticator <- env.authenticatorService.create(loginInfo)
                result <- env.authenticatorService.renew(authenticator, Redirect(routes.ApplicationCtrl.myAccount).flashing("success" -> Messages("auth.password.changed")))
              } yield result
          }.recover {
            case e: ProviderException => BadRequest(viewsAuth.changePassword(changePasswordForm.withError("current", Messages("auth.currentpwd.incorrect"))))
          }
        }
      )
  }

  /**
   * Shows an error page when the manager tries to get to an area without the necessary roles.
   */
  def accessDenied = SecuredAction { implicit request =>
    Ok(viewsAuth.accessDenied())
  }

  def confirmAccount(token: String) = Action.async { implicit request =>
    Future.successful {
      val user = Users.find.where().eq("activation_token", token).findUnique()
      user match {
        case u: Users =>
          u.setIsVerify(YesNoEnum.Yes)
          u.update()
          Ok("Account Verified Successfully")
        case _ => Ok("Invalid activation token.")
      }
    }
  }

  @Transactional
  def handleStartSignUp = UserAwareAction.async { implicit request =>
    signUpForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(viewsAuth.signUp(formWithErrors))),
      user => {
        val loginInfo: LoginInfo = user.email
        env.identityService.retrieve(loginInfo).flatMap {
          case Some(_) => Future.successful(BadRequest(viewsAuth.signUp(signUpForm.withError("email", Messages("web.auth.user.notunique")))))
          case None => for {
            savedUser <- userService.b2bCreateAccount(user)
            _ <- Future.successful(Mailer.agentSignUp(savedUser, link = routes.AuthCtrl.confirmAccount(savedUser.getActivationToken).absoluteURL()))
            _ <- Future.successful {
              val privateUsers = PrivateUsers.find.where().in("u_roles", Roles.agent_manager.name(), Roles.operation_manager.name(), Roles.sale_manager.name(), Roles.super_admin.name()).findList()
              privateUsers.foreach { recipient =>
                val msgMail = views.html.mails.notification("B2B Sign Up", s"${savedUser.getAgentCorporateDetailId.getCompanyName} Signed Up on B2B. Kindly logon to review and action.", recipient).body
                mailService.sendEmailAsync(recipient.getEmail, "catueyi@vectatravels.com", "fdibia@vectatravels.com", "aakanbi@vectatravels.com")("B2B Sign Up", msgMail, msgMail)
              }
            }
            result <- Future.successful(Ok(viewsAuth.almostSignedUp(savedUser)))
          } yield result
        }
      }
    )
  }

}