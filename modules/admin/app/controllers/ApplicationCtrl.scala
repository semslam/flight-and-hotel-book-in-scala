package controllers.admin

import java.util.Calendar

import com.avaje.ebean.{ Ebean, RawSqlBuilder }
import crypto.Encrypt
import models._
import org.joda.time.LocalDate
import utils.silhouette._
import play.api._
import play.api.mvc._
import play.api.i18n.{ Lang, Messages, MessagesApi }
import views.html.admin.flightBookings

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import play.cache.Cached

import scala.collection.JavaConverters._
import models.admin.MailTokenManager
import utils.silhouette.admin.{ AdminController, ManagerService, WithRole, WithRoles }

case class FlightRelatedQueues(paymentCatQueues: Seq[(PaymentCategories, List[PaymentHistories])])

case class DashboardDAO(flightRelatedQueues: FlightRelatedQueues)

class ApplicationCtrl @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi, encrypt: Encrypt) extends AdminController {

  implicit val enc = encrypt

  @Cached(key = "admin.index.dashboard")
  def index = SecuredAction { implicit request =>
    Ok(views.html.admin.index())
  }

  def myAccount = SecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.admin.myAccount()))
  }

  def chartData(dtype: String) = SecuredAction { implicit request =>
    var toJson = Map[String, Any]()
    dtype match {
      case "monthOfYear" =>
        val calendar = Calendar.getInstance()
      case "dayOfMonth" =>
    }
    Ok("")
  }

  // REQUIRED ROLES: social (or master)
  def social = SecuredAction(WithRole("social")).async { implicit request =>
    Future.successful(Ok(views.html.admin.social()))
  }

  // REQUIRED ROLES: sales OR high (or master)
  def salesOrHigh = SecuredAction(WithRole("sales", "high")).async { implicit request =>
    Future.successful(Ok(views.html.admin.salesOrHigh()))
  }

  // REQUIRED ROLES: sales AND high (or master)
  def salesAndHigh = SecuredAction(WithRoles("sales", "high")).async { implicit request =>
    Future.successful(Ok(views.html.admin.salesAndHigh()))
  }

  // REQUIRED ROLES: master
  def settings = SecuredAction(WithRole("master")).async { implicit request =>
    Future.successful(Ok(views.html.admin.settings()))
  }

  def selectLang(lang: String) = Action { implicit request =>
    Logger.logger.debug("Change user lang to : " + lang)
    request.headers.get(REFERER).map { referer =>
      Redirect(referer).withLang(Lang(lang))
    }.getOrElse {
      Redirect(routes.ApplicationCtrl.index).withLang(Lang(lang))
    }
  }

  //TODO
  def cookieUID(cookie_id: String, redirect_to: String) = Action { implicit request =>
    Ok("")
    //    Redirect(redirect_to).withCookies(play.api.mvc.Cookie(cookie.cookieName, cookie_id, Some(cookie.cookieDuration.toSeconds.toInt)))
  }

  def appSearch = SecuredAction { implicit request =>
    val search = s"%${request.getQueryString("search").getOrElse("")}%"
    val sql = s"select distinct  flb.id, bok.auth_operator_id, bok.transaction_ref, flb.pnr_ref, bok.sales_category, bok.contact_title, bok.contact_firstname, bok.contact_surname, bok.contact_email, bok.contact_phone, flb.airline_code, flb.cabin_class, bok.status, bok.payment_history_id, flb.created_at from flight_bookings flb left join bookings bok on flb.bookings = bok.id  join flight_booking_pnr u1 on u1.flight_booking_id = flb.id  left outer join users t1 on t1.id = bok.user_id  where flb.deleted != 1  and bok.transaction_ref like '%$search%' escape''  or flb.pnr_ref like '%$search%' escape''  or u1.middle_name like '%$search%' escape'' or u1.first_name like '%$search%' escape''  or u1.last_name like '%$search%' escape'' OR bok.contact_email like '%$search%' escape''  or bok.contact_phone like '%$search%' escape'' OR t1.phone like '%$search%' escape''  or t1.email like '%$search%' escape'' order by flb.id desc "
    val rawSql = RawSqlBuilder.parse(sql)
      .tableAliasMapping("flb", "flight_bookings").tableAliasMapping("u1", "flight_booking_pnr").tableAliasMapping("t1", "users")
      .columnMapping("flb.id", "id").columnMapping("bok.auth_operator_id", "bookings.authOperatorId.id").columnMapping("bok.transaction_ref", "bookings.transactionRef")
      .columnMapping("flb.pnr_ref", "pnrRef").columnMapping("bok.sales_category", "bookings.salesCategory").columnMapping("bok.contact_title", "bookings.contactTitle")
      .columnMapping("bok.contact_firstname", "bookings.contactFirstname").columnMapping("bok.contact_surname", "bookings.contactSurname").columnMapping("bok.contact_email", "bookings.contactEmail")
      .columnMapping("bok.contact_phone", "bookings.contactPhone").columnMapping("flb.airline_code", "airlineCode").columnMapping("flb.cabin_class", "cabinClass")
      .columnMapping("bok.status", "bookings.status").columnMapping("bok.payment_history_id", "bookings.paymentHistoryId.id").columnMapping("flb.created_at", "createdAt")
      .create()
    val results = Ebean.createQuery(classOf[FlightBookings]).setRawSql(rawSql).findList()
    Ok(flightBookings.bookingList("Booking Queues", results.asScala.toList))
  }

  def bugReport = SecuredAction { implicit request =>
    Ok(views.html.admin.bugReport())
  }

  def submitBug = SecuredAction { implicit request =>
    Ok(views.html.admin.bugReport())
  }

}