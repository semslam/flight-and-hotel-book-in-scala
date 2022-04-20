package controllers.b2b

import java._
import java.text.SimpleDateFormat

import javax.inject._
import api.ServicesAPI
import com.alajobi.ota.flights.HttpRequestParser
import com.alajobi.ota.utils.SupplierManager.Implicits._
import com.alajobi.ota.utils.SupplierManager.{ Implicits => HotelConfig }
import com.alajobi.ota.utils.{ CachedVariables, Pagination }
import com.avaje.ebean.{ Ebean, Expr, RawSql, RawSqlBuilder }
import com.google.common.base.Strings
import com.hotels._
import com.mohiva.play.silhouette.api.Silhouette
import crypto.{ Encrypt, TransRefAlgorithm }
import hotel.dto.entity._
import hotel.dto.entity.booking._
import mailer.MailService
import models._
import play.api.i18n.MessagesApi
import play.api.libs.json.{ JsNumber, JsObject, JsString }
import play.api.mvc._
import utils.silhouette.MyEnv
import views.html.b2b._

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.data.Form
import play.api.data.Forms._
import play.api.data._
import utils.silhouette.b2b.B2BController

case class PackageBookingFormDAO(packageId: String, duration: String, startDate: String, childCount: Int, adultCount: Int, contactTitle: String, contactFirstName: String, contactLastName: String, contactPhone: String, contactEmail: String, remark: Option[String])

class PackageCtrl @Inject() (val silhouette: Silhouette[MyEnv[Users]], val messagesApi: MessagesApi, serviceAPI: ServicesAPI, fareRuleService: HotelFareRuleService, hotelCRUD: HotelCRUD, implicit val encrypt: Encrypt, implicit val mailService: MailService) extends B2BController {

  val bookingForm = Form(mapping(
    "packageId" -> nonEmptyText,
    "duration" -> nonEmptyText,
    "startDate" -> nonEmptyText,
    "childCount" -> number,
    "adultCount" -> number,
    "contactTitle" -> nonEmptyText,
    "contactFirstName" -> nonEmptyText,
    "contactLastName" -> nonEmptyText,
    "contactPhone" -> nonEmptyText,
    "contactEmail" -> nonEmptyText,
    "remark" -> optional(nonEmptyText)
  )(PackageBookingFormDAO.apply)(PackageBookingFormDAO.unapply))

  def noResult = SecuredAction(implicit request => Ok(packages.error()))

  def search = SecuredAction { implicit request =>
    val adults = request.getQueryString("num_of_adult").get.toInt
    val children = request.getQueryString("num_of_children").get.toInt
    val packagez = computePackage(packagez = Packages.find.where().raw(s"countryId = ${request.getQueryString("country_id").orNull} ").findList().toVector, adults, children)
    Ok("")
  }

  def vacationPackages(page: Int, apply_filter: Boolean, country_id: Long, theme_id: String, pick_up: String, num_of_adult: Int, num_of_children: Int) = SecuredAction.async { implicit request =>
    Future {
      val pageLength = 15
      if (apply_filter) {
        var query = Packages.find.where().eq("country_id", country_id)
        if (!Strings.isNullOrEmpty(theme_id)) query = query.where().eq("package_theme_id", theme_id)
        val offset = (page - 1) * pageLength
        val expression = query.where().raw(s" deleted = 0 order by id desc limit $pageLength offset $offset ").setIncludeSoftDeletes()
        Ok(views.html.b2b.packages.result(expression.findList.toVector, Packages.find.where().eq("country_id", country_id).findRowCount(), page, pageLength, apply_filter, country_id, theme_id))
      } else {
        val offset = (page - 1) * pageLength
        val expression = Packages.find.where().raw(s" deleted = 0 order by id desc limit $pageLength offset $offset ").setIncludeSoftDeletes()
        Ok(views.html.b2b.packages.result(expression.findList.toVector, Packages.find.findRowCount(), page, pageLength, apply_filter, country_id, theme_id))
      }
    }
  }

  def select(id: String) = SecuredAction { implicit request =>
    Ok(views.html.b2b.packages.select(Packages.find.byId(encrypt.decrypt(id).toLong), bookingForm))
  }

  def book(packageId: String) = SecuredAction { implicit request =>
    bookingForm.bindFromRequest().fold(
      error => {
        BadRequest(views.html.b2b.packages.select(Packages.find.byId(encrypt.decrypt(packageId).toLong), error))
      },
      success => {
        val conf = play.Configuration.root()
        val booking = new Bookings
        booking.setUserId(request.identity)
        booking.setContactEmail(success.contactEmail)
        booking.setContactPhone(success.contactPhone)
        booking.setContactTitle(success.contactTitle)
        booking.setContactFirstname(success.contactFirstName)
        booking.setContactSurname(success.contactLastName)
        booking.setStatus(BookingStatus.CONFIRMED)
        booking.setSupplier(ApiSuppliers.find.where().eq("name", conf.getString("project.supplier")).findUnique())
        booking.setTransactionRef(TransRefAlgorithm.getUniqueRef(SalesCategory.B2B).getRefCode)
        booking.setSalesCategory(SalesCategory.B2B)
        booking.insert()

        val dateFormat = new SimpleDateFormat("MM/dd/yyyy")
        //Save the package
        val packageBooking = new PackageBookings
        packageBooking.setBookings(booking)
        packageBooking.setCheckIn(dateFormat.parse(success.startDate))
        packageBooking.setDuration(success.duration.toString)
        packageBooking.setNoOfAdult(success.adultCount.toString)
        packageBooking.setNoOfChild(success.childCount.toString)
        packageBooking.setPackageId(Packages.find.byId(encrypt.decrypt(success.packageId).toLong))
        packageBooking.insert()
        mailService.sendEmailAsync(conf.getString("project.email"))("Package Booking", views.html.mails.packageBookingNotification(packageBooking).body, views.html.mails.packageBookingNotification(packageBooking).body)
        Ok(views.html.b2b.packages.thankyou(packageBooking))
      }
    )
  }

  private def computePackage(packagez: Vector[Packages], adult: Int, child: Int) = packagez.map { item =>
    val totalPrice = (item.getAdultUnitPrice * adult) + (item.getChildUnitPrice * child)
    item.setPriceCaptionDescription(totalPrice.toString)
    item
  }

}

