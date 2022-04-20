package controllers.web

import java.net.URLEncoder

import crypto.Encrypt
import cms.DynamicPageHandler
import models._
import play.api.libs.json.{ JsNumber, JsObject }
import play.twirl.api.Html
import utils.silhouette._
import play.api.mvc._
import play.api.i18n.{ Lang, Messages, MessagesApi }
import views.html.web.errors.notFound
import views.html.web.pages.app

import scala.concurrent.Future
import javax.inject.Inject

import com.avaje.ebean.Ebean
import com.mohiva.play.silhouette.api.Silhouette
import play.api.libs.ws.WSClient
import mailer.MailService
import play.cache.Cached
import play.api.data._
import play.api.data.Forms._
import views.html.paymentapi.gtpay

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import play.api.libs.json._

case class VisaFormDAO(fullName: String, phone: String, email: String, dDate: String, aDate: String, residentCountry: String, arrivingCountry: String, message: Option[String])

case class KycFormDAO(fullName: String, phone: String, email: Option[String], interest: String)

class ApplicationCtrl @Inject() (val silhouette: Silhouette[MyEnv[Users]], val messagesApi: MessagesApi, val mailService: MailService, wSClient: WSClient, implicit val encrypt: Encrypt) extends WebController {

  @Cached(key = "index")
  def index = page("/")

  def kycIndex = UserAwareAction { implicit request =>
    Ok(views.html.web.kycpage(kycPage))
  }

  val form = Form(
    mapping(
      "fullName" -> nonEmptyText,
      "phone" -> nonEmptyText,
      "email" -> nonEmptyText,
      "dDate" -> nonEmptyText,
      "aDate" -> nonEmptyText,
      "residentCountry" -> nonEmptyText,
      "arrivingCountry" -> nonEmptyText,
      "message" -> optional(nonEmptyText)
    )(VisaFormDAO.apply)(VisaFormDAO.unapply)
  )
  val kycPage = Form(
    mapping(
      "fullName" -> nonEmptyText,
      "phone" -> nonEmptyText,
      "email" -> optional(nonEmptyText.verifying("Invalid Email Specified.", email => "^((\"[\\w-\\s]+\")|([\\w-]+(?:\\.[\\w-]+)*)|(\"[\\w-\\s]+\")([\\w-]+(?:\\.[\\w-]+)*))(@((?:[\\w-]+\\.)*\\w[\\w-]{0,66})\\.([a-z]{2,6}(?:\\.[a-z]{2})?)$)|(@\\[?((25[0-5]\\.|2[0-4][0-9]\\.|1[0-9]{2}\\.|[0-9]{1,2}\\.))((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[0-9]{1,2})\\.){2}(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[0-9]{1,2})\\]?$)".r.findAllIn(email).hasNext)),
      "interest" -> nonEmptyText
    )(KycFormDAO.apply)(KycFormDAO.unapply)
  )

  def visa = UserAwareAction { implicit request =>
    Ok(views.html.web.visa(form))
  }

  def exclusiveOffer = UserAwareAction { implicit request =>
    Ok(views.html.web.exclusiveOffers())
  }

  def postVisa = UserAwareAction { implicit request =>
    form.bindFromRequest().fold(
      error => BadRequest(views.html.web.visa(error)),
      success => {
        val formMsg = views.html.web.mails.visaEnquiry(success).toString()
        mailService.sendEmailAsync(play.Configuration.root().getString("project.email"))("Visa Application", formMsg, formMsg)
        Redirect(routes.ApplicationCtrl.visa()).flashing("success" -> "Thank you! Your visa request has been submitted. We'll get back to you.")
      }
    )

  }

  def postKyc = UserAwareAction { implicit request =>
    kycPage.bindFromRequest().fold(
      error => BadRequest(views.html.web.kycpage(error)),
      success => {
        Ebean.createSqlUpdate(s"INSERT INTO `kyc_form` (`full_name`, `email`, `phone`, `interest`, `deleted`) VALUES ('${success.fullName}', '${success.email.orNull}', '${success.phone}', '${success.interest}', 0)").execute()
        Redirect(routes.ApplicationCtrl.kycIndex()).flashing("success" -> "Thank you! Your feedback's well appreciated.")
      }
    )

  }

  def saveVisitor(name: String, email: String) = UserAwareAction { implicit request =>
    val newsletterUser = new NewsletterUsers
    newsletterUser.setEmail(email)
    newsletterUser.setFirstName(name)
    newsletterUser.insert()
    Ok(JsObject(Map(
      "responseCode" -> JsNumber(200),
      "region" -> JsString("Newsletter subscribed successful")
    )))
  }

  def page(url: String) = UserAwareAction.async { implicit request =>
    Future.successful {
      import scala.collection.JavaConversions._
      for (corporateDetails <- AgentCorporateDetails.find.all) {
      }
      val path = request.path
      var host = request.host
      var response = NotFound(notFound(request = request))
      if (host.toLowerCase().contains("www.")) host = host.split("www.").apply(1)
      Option(CmsPages.find.where.eq("slug_url", path).findUnique()) match {
        case page: Some[CmsPages] =>
          val templatesManager = new DynamicPageHandler(page.get)
          templatesManager.getAttrValue("description")
          response = Ok(app(page.get, templatesManager, request.identity))
        case _ => response = Ok(views.html.web.errors.notFound(request))
      }
      response
    }
  }

  //TODO  -> Set Cookie
  def cookieUID(cookie_id: String, redirect_to: String) = Action { implicit request =>
    Ok("")
    //    Redirect(redirect_to).withCookies(play.api.mvc.Cookie(cookie.cookieName, cookie_id, Some(cookie.cookieDuration.toSeconds.toInt)))
  }

  def selectLang(lang: String) = Action { implicit request =>
    println("Change user lang to :", lang)
    request.headers.get(REFERER).map { referer =>
      Redirect(referer).withLang(Lang(lang))
    }.getOrElse {
      Redirect(routes.ApplicationCtrl.index).withLang(Lang(lang))
    }
  }

  def priceAlertSubscribe = Action { implicit request =>
    Ok(views.html.web.priceAlert())
  }

  def newsletterSubscribe = Action { implicit request =>
    var status = 0
    val email = request.body.asFormUrlEncoded.get("email").head
    val fullname = if (request.body.asFormUrlEncoded.get.contains("fullname")) request.body.asFormUrlEncoded.get("fullname").head else ""
    NewsletterUsers.find.where().eq("email", email).findUnique() match {
      case null =>
        status = 1
        val newsletterUser = new NewsletterUsers
        newsletterUser.setEmail(email)
        newsletterUser.setFirstName(fullname)
        newsletterUser.insert()
      case _ => status = 0
    }
    Ok(JsObject(Map(
      "status" -> JsNumber(status)
    )))
  }

  def testPagexx = UserAwareAction.async {
    wSClient.url("https://iatacodes.org/api/v6/airlines?api_key=6ea25e51-cb44-4f6b-a402-0ebf38f2a4ae")
      .withHeaders("Accept" -> "application/json")
      .withRequestTimeout(30 seconds).execute().map { response =>
        val code = response.json \ "response" \\ "code"
        val name = response.json \ "response" \\ "name"
        var index = 0
        code.foreach { mapItem =>
          val c = mapItem.toString().replaceAll("\"", "").toUpperCase
          Option(Airlines.find.where().eq("airlineCode", c).findUnique()) match {
            case Some(_) =>
            case _ =>
              val db = new Airlines()
              //            db.airlineCode = c
              //            db.name = name(index).toString().replaceAll("\"", "")
              db.insert()
          }
          index += 1
        }
        code.map(a => println(a.toString()))

        Ok("hello world!")
      }
    //    FlightScrapper.getFlight().map { html =>
    //      Ok(Html(html))
    //    }
  }

  def testPage = UserAwareAction {
    utils.SmsSender.sendSms(URLEncoder.encode(messagesApi("flight.sms.confirm", "Igbalajobi", "#12345677")), "Company", "08127119051")

    Ok("")
    //    wSClient.url("http://localhost:9001/common/cities.json").withHeaders("Accept" -> "application/json").withRequestTimeout(30 seconds).execute().map { response =>
    //      val code = response.json \\ "Region ID"
    //      val name = response.json \\ "Region Name"
    //      val country = response.json \\ "Country Name"
    //      var index = 0
    //      country foreach { item =>
    //        val countryRelaceStr = Map("UK - England" -> "United Kingdom", "USA" -> "United States", "UAE" -> "United Arab Emirates")
    //        val countryName = item.toString().replaceAll("\"", "")
    //        var countryTbl = Countries.find.where().eq("name", countryName).findUnique
    //        if (countryTbl == null)
    //          countryTbl = Countries.find.where().eq("name", countryRelaceStr(countryName)).findUnique
    //
    //        val hotelDestination = new HotelDestinations
    //        hotelDestination.setCode(code(index).toString().replaceAll("\"", ""))
    //        hotelDestination.setName(name(index).toString().replaceAll("\"", ""))
    //        hotelDestination.setCountry(countryTbl)
    //        hotelDestination.setSupplier(models.ApiSuppliers.find.where().eq("name", "roomxml").findUnique())
    //        //        hotelDestination.save()
    //        index += 1
    //      }
    //      Ok("hello world!")
    //    }
  }

  def testApiCall(pnrRef: String) = UserAwareAction { implicit request =>
    val supplierDest = models.HotelDestinations.find.all().asScala.map { dest =>
      var item = Map[String, Any]()
      item += ("id" -> dest.getId)
      item += ("supplier" -> dest.getSupplier.getName)
      item += ("destination_type" -> dest.getDestinationType)
      item += ("code" -> dest.getCode)
      item += ("name" -> dest.getName)
      item += ("country" -> Map("id" -> dest.getCountry.getId, "name" -> dest.getCountry.getName, "capital" -> dest.getCountry.getIsoCode3).asJava)
      item.asJava
    }
    println(play.libs.Json.toJson(supplierDest.asJava))
    Ok("")
    //    Ok(gtpay(PaymentHistories.find.byId(4), "Jamiu", "Igbalajobi"))
  }

}