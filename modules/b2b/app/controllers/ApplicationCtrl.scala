package controllers.b2b

import java.net.URLEncoder

import com.alajobi.ota.utils.Cookie
import crypto.Encrypt
import aws._
import models._
import utils.silhouette._
import play.api._
import play.api.mvc._
import play.api.i18n.{ Lang, Messages, MessagesApi }

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.cache.Cached
import utils.silhouette.b2b._

class ApplicationCtrl @Inject() (val silhouette: Silhouette[MyEnv[Users]], val messagesApi: MessagesApi, cookie: Cookie, s3Client: AwsS3Services, encrypt: Encrypt) extends B2BController {

  implicit val enc = encrypt

  @Cached(key = "b2b.index")
  def index = SecuredAction { implicit request =>
    Ok(views.html.b2b.index("Dashboard", FlightBookings.find.where().eq("bookings.userId", request.identity)))
  }

  def myAccount = SecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.b2b.myAccount()))
  }

  def selectLang(lang: String) = Action { implicit request =>
    Logger.logger.debug("Change user lang to : " + lang)
    request.headers.get(REFERER).map { referer =>
      Redirect(referer).withLang(Lang(lang))
    }.getOrElse {
      Redirect(routes.ApplicationCtrl.index).withLang(Lang(lang))
    }
  }

  def cookieUID(cookie_id: String, redirect_to: String) = Action { implicit request =>
    Redirect(redirect_to).withCookies(play.api.mvc.Cookie(cookie.cookieName, cookie_id, Some(cookie.cookieDuration.toSeconds.toInt)))
  }

  val formReport = Form(tuple(
    "from" -> nonEmptyText,
    "to" -> nonEmptyText,
    "status" -> nonEmptyText
  ))

  def report = SecuredAction { implicit request =>
    val flightData = request.identity.getRole match {
      case "b2b_owner" => Bookings.find.where().eq("userId.agentCorporateDetailId", request.identity.getAgentCorporateDetailId)
      case _ => Bookings.find.where().eq("userId", request.identity)
    }
    val results = flightData.order().desc("id").findList()
    Ok(views.html.b2b.report(results.asScala.toList, formReport))
  }

  def editProfile = SecuredAction { implicit request =>
    Ok(views.html.b2b.editProfile())
  }

  def uploadLogo = SecuredAction.async { implicit request =>
    Future.successful {
      if (request.body.asMultipartFormData.isDefined) {
        request.body.asMultipartFormData.get.files.map { file =>
          s3Client.s3FilesClient.uploadFile(file).map(_.map(uploadedFile => {
            //save the record to the database.
            request.identity.getAgentCorporateDetailId.setLogoUrl(s"${s3Client.awsAuth.endpointUrl}${URLEncoder.encode(file.filename, "UTF-8")}")
            request.identity.getAgentCorporateDetailId.update()
            uploadedFile
          }))
        }
      }
      Redirect(routes.ApplicationCtrl.myAccount()).withNewSession.flashing(("success", "Logo Uploaded Successfully"))
    }
  }

  def genInvoice(format: String) = SecuredAction { implicit request =>
    Ok("")
  }

  def genReport = SecuredAction { implicit request =>
    Ok("")
  }

  def viewBookingItinerary(ref: String) = SecuredAction { implicit request =>
    Ok("")
  }

}