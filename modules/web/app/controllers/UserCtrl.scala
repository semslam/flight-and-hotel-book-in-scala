package controllers.web

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import models.{ Bookings, Users }
import play.api.i18n.MessagesApi
import utils.silhouette.{ MyEnv, WebController }
import views.html.web._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

/**
 * Created by
 * Igbalajobi Jamiu O. on 14/05/2016 4:37 PM.
 */
class UserCtrl @Inject() (val silhouette: Silhouette[MyEnv[Users]], val messagesApi: MessagesApi, implicit val encrypt: crypto.Encrypt) extends WebController {

  def dashboard = SecuredAction { implicit request =>
    Ok(user.dashboard())
  }

  def myAccount = SecuredAction { implicit request =>
    Ok(user.myAccount())
  }

  def editProfile = SecuredAction { implicit request =>

    Ok(user.editProfile())
  }

  def postEditProfile = SecuredAction { implicit request =>
    Ok("")
  }

  def bookings = SecuredAction { implicit request =>
    Ok(user.bookings(Bookings.find.where().eq("userId", request.identity).findList().asScala.toList))
  }

  def flightDetail(ref: String) = SecuredAction { implicit request =>
    Ok("")
  }

}
