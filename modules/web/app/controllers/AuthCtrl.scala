package controllers.web

import models._
import utils.silhouette._
import utils.silhouette.Implicits._
import com.mohiva.play.silhouette.api.{ LoginEvent, LoginInfo, LogoutEvent, SignUpEvent, Silhouette }
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.{ Clock, Credentials, PasswordHasherRegistry }
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.exceptions.{ IdentityNotFoundException }
import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.i18n.{ Messages, MessagesApi }
import utils.web.Mailer

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import net.ceedubs.ficus.Ficus._
import javax.inject.{ Inject, Singleton }

import com.google.inject.name.Named
import crypto.{ Encrypt, Hash }
import mailer.MailService
import play.api.libs.json.{ JsString, JsNumber, JsObject }
import views.html.web.{ auth => viewsAuth }

case class UserSignUp(id: Option[Long], firstName: String, lastName: String, email: String, phone: Option[String], password: String, rePassword: String)

@Singleton
class AuthCtrl @Inject() (
  val silhouette: Silhouette[MyEnv[Users]],
  val messagesApi: MessagesApi, userService: UserService,
  @Named("auth-info-repository") authInfoRepository: AuthInfoRepository,
  @Named("credentials-provider") credentialsProvider: CredentialsProvider,
  passwordHasherRegistry: PasswordHasherRegistry,
  mailService: MailService, conf: Configuration,
  clock: Clock,
  implicit val encrypt: Encrypt
)
    extends WebController {

  // UTILITIES
  implicit val ms = mailService

  val passwordValidation = nonEmptyText(minLength = 6)

  def notFoundDefault(implicit request: RequestHeader) = Future.successful {
    implicit val user: Option[Users] = None
    NotFound(views.html.web.errors.notFound(request))
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // SIGN UP

  val signUpForm = Form(
    mapping(
      "id" -> ignored(None: Option[Long]),
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText,
      "email" -> email.verifying("Ooops! Email already exists", email => Users.find.where().eq("email", email).findRowCount() == 0),
      "phone" -> optional(nonEmptyText.verifying(maxLength(11))),
      "password" -> nonEmptyText.verifying(minLength(1)),
      "rePassword" -> nonEmptyText
    )(UserSignUp.apply)(UserSignUp.unapply).verifying("Password did not match", data => data.password == data.rePassword)
  )

  /**
   * Starts the sign up mechanism. It shows a form that the user have to fill in and submit.
   */
  def startSignUp = UserAwareAction { implicit request =>
    request.identity match {
      case Some(_) => Redirect(routes.ApplicationCtrl.index)
      case None => Ok(viewsAuth.signUp(signUpForm))
    }
  }

  /**
   * Confirms the user's email address based on the token and authenticates him.
   */
  def signUp(tokenId: String) = UserAwareAction.async { implicit request =>
    userService.activateToken(tokenId) flatMap {
      case Some(users) => for {
        authenticator <- env.authenticatorService.create(LoginInfo(users.getId.toString, users.getEmail)).map(authenticatorWithRememberMe(_, true))
        cookie <- env.authenticatorService.init(authenticator)
        result <- env.authenticatorService.embed(cookie, Redirect(routes.ApplicationCtrl.index))
      } yield {
        env.eventBus.publish(LoginEvent(users, request))
        result
      }
      case _ => Future.failed(new IdentityNotFoundException("Couldn't find user"))
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // SIGN IN

  val signInForm = Form(tuple(
    "email" -> email,
    "password" -> nonEmptyText,
    "rememberMe" -> boolean
  ))

  /**
   * Starts the sign in mechanism. It shows the login form.
   */
  def signIn = UserAwareAction { implicit request =>
    request.identity match {
      case Some(_) => Redirect(routes.ApplicationCtrl.index())
      case None => Ok(viewsAuth.signIn(signInForm))
    }
  }

  def bloggerSignIn = UserAwareAction { implicit request =>
    request.identity match {
      case Some(_) => Redirect(routes.PackageCtrl.vacationPackages())
      case None => Ok(viewsAuth.bloggerSignIn(signInForm))
    }
  }

  /**
   * Confirms the user's email address based on the token and authenticates him.
   */
  def bloggerSignUp = UserAwareAction { implicit request =>
    request.identity match {
      case Some(_) => Redirect(routes.ApplicationCtrl.index)
      case None => Ok(viewsAuth.bloggerSignUp(signUpForm))
    }
  }

  /**
   * Authenticates the user based on his email and password
   */
  def authenticate = UnsecuredAction.async {
    implicit request =>
      signInForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(viewsAuth.signIn(formWithErrors))),
        formData => {
          val (email, password, rememberMe) = formData
          credentialsProvider.authenticate(Credentials(email, password)).flatMap {
            loginInfo =>
              userService.retrieve(loginInfo).flatMap {
                case Some(user) =>
                  if (user.getRole.equalsIgnoreCase(Roles.b2c_customer.name())) {
                    for {
                      authenticator <- env.authenticatorService.create(loginInfo).map(authenticatorWithRememberMe(_, rememberMe))
                      cookie <- env.authenticatorService.init(authenticator)
                      result <- env.authenticatorService.embed(cookie, Ok(JsObject(Map("status" -> JsNumber(200), "msg" -> JsString("Login Successful, redirecting...please wait!")))))
                    } yield {
                      env.eventBus.publish(LoginEvent(user, request))
                      result
                    }
                  } else {
                    Future(Ok(JsObject(Map("status" -> JsNumber(500), "msg" -> JsString("Please login with your agent portal. Sign In not allowed here")))))
                  }
                case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
              }
          }.recover {
            case e: ProviderException => Ok(JsObject(Map("status" -> JsNumber(500), "msg" -> JsString("Failed! either email or password is incorrect."))))
          }
        }
      )
  }

  /**
   * Handles the form filled by the user. The user and its password are saved and it sends him an email with a link to confirm his email address.
   */
  def handleStartSignUp = UnsecuredAction.async {
    implicit request =>
      signUpForm.bindFromRequest.fold(
        error => Future.successful(BadRequest(viewsAuth.signUp(error))),
        form => {
          userService.retrieve(form.email).flatMap {
            case Some(_) => Future.successful(Ok(JsObject(Map("status" -> JsNumber(500), "msg" -> JsString("Sorry! Email address already exists")))))
            case _ => for {
              savedUser <- userService.b2cCreateAccount(form.firstName, form.lastName, form.email, form.phone, form.password, form.rePassword, form.id)
              _ <- Future.successful(Mailer.welcome(savedUser, link = routes.AuthCtrl.signUp(savedUser.getActivationToken).absoluteURL()))
            } yield Ok(JsObject(Map("status" -> JsNumber(200), "msg" -> JsString("Thank you! Registration successful"), "url" -> JsString(routes.ApplicationCtrl.index().absoluteURL())))) // Ok(viewsAuth.almostSignedUp(savedUser))
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
   * Signs out the user
   */
  def signOut = SecuredAction.async {
    implicit request =>
      env.eventBus.publish(LogoutEvent(request.identity, request))
      env.authenticatorService.discard(request.authenticator, Redirect(routes.ApplicationCtrl.index))
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // FORGOT PASSWORD

  val emailForm = Form(single("email" -> email))

  /**
   * Starts the reset password mechanism if the user has forgot his password. It shows a form to insert his email address.
   */
  def forgotPassword = UserAwareAction {
    implicit request =>
      request.identity match {
        case Some(_) => Redirect(routes.ApplicationCtrl.index)
        case None => Ok(viewsAuth.forgotPassword(emailForm))
      }
  }

  /**
   * Sends an email to the user with a link to reset the password
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
   * Confirms the user's link based on the token and shows him a form to reset the password
   */
  def resetPassword(tokenId: String) = UnsecuredAction.async { implicit request =>
    userService.activateToken(tokenId).flatMap {
      case Some(token) => Future.successful(Ok(viewsAuth.resetPassword(tokenId, resetPasswordForm)))
      case None => notFoundDefault
    }
  }

  /**
   * Saves the new password and authenticates the user
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
  def changePassword = SecuredAction {
    implicit request =>
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
                result <- env.authenticatorService.renew(authenticator, Redirect(routes.UserCtrl.myAccount).flashing("success" -> Messages("auth.password.changed")))
              } yield result
          }.recover {
            case e: ProviderException => BadRequest(viewsAuth.changePassword(changePasswordForm.withError("current", Messages("auth.currentpwd.incorrect"))))
          }
        }
      )
  }
}