package controllers.admin.management

import java._
import java.text.SimpleDateFormat

import api.ServicesAPI
import com.hotels.HotelCRUD
import com.mohiva.play.silhouette.api.Silhouette
import crypto.Encrypt
import hotel.dto.entity.booking.{ CancelHotelRequest, Order, OrderPrice, QueryHotelRequest }
import hotel.dto.entity.{ HotelAvailabilityRequest, Room }
import hotel.dto.enums.CRoomtype
import javax.inject.{ Inject, Singleton }
import mailer.MailService
import models._
import play.api.i18n.MessagesApi
import play.api.mvc.RequestHeader
import utils.SupplierConfigManager
import utils.silhouette.MyEnv
import utils.silhouette.admin.AdminController
import views.html.admin.{ packageBookings, _ }

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by Igbalajobi Jamiu Okunade on 5/21/17.
 */
@Singleton
class PackageBookingCtrl @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi, implicit val encrypt: Encrypt, mailService: MailService, services: ServicesAPI, hotelCRUD: HotelCRUD) extends AdminController {

  def fromUrl(implicit requestHeader: RequestHeader) = requestHeader.headers.get(REFERER).getOrElse(controllers.admin.management.routes.HotelBookingCtrl.index().absoluteURL())

  def index(qType: String) = SecuredAction { implicit request =>
    val bookingLists = qType match {
      case "packageQueues.archieved" => PackageBookings.find.where().eq("bookings.isArchived", false).findList()
      //      case "hotelQueues.bookingByStatus.failed" => HotelBookings.find.where().eq("bookings.isArchived", false).where().eq("bookings.status", BookingStatus.FAILED_BOOKING).findList()
      //      case "hotelQueues.bookingByStatus.cancelled" => HotelBookings.find.where().eq("bookings.isArchived", false).where().eq("bookings.status", BookingStatus.CANCELLED).findList()
      //      case "hotelQueues.bookingByStatus.archived" => HotelBookings.find.where().eq("bookings.isArchived", true).findList()
      case _ => PackageBookings.find.all()
    }
    Ok(packageBookings.index(bookingLists.asScala.toList))
  }

  def manage(ref: String) = SecuredAction { implicit request =>
    val packageBookings = PackageBookings.find.where().eq("bookings.transactionRef", encrypt.decrypt(ref)).findUnique()
    //Log the user to this booking
    packageBookings.getBookings.setAuthOperatorId(request.identity)
    packageBookings.getBookings.setAuthIsReadable(true)
    packageBookings.getBookings.setAuthIsWriteable(false)
    packageBookings.getBookings.update()
    Ok(views.html.admin.packageBookings.manage(packageBookings))
  }
}
