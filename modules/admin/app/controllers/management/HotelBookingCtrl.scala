package controllers.admin.management

import java.text.SimpleDateFormat
import javax.inject.{ Inject, Singleton }

import api.ServicesAPI
import com.mohiva.play.silhouette.api.Silhouette
import com.hotels.HotelCRUD
import crypto.Encrypt
import hotel.dto.entity.booking.{ Order, OrderPrice, CancelHotelRequest, QueryHotelRequest }
import hotel.dto.entity.{ Room, HotelAvailabilityRequest }
import hotel.dto.enums.CRoomtype
import models._
import play.api.i18n.MessagesApi
import mailer.MailService
import play.api.mvc.{ RequestHeader, Request }
import utils.SupplierConfigManager
import utils.silhouette.MyEnv
import views.html.admin._
import utils.silhouette.admin.{ AdminController, ManagerService, WithRole, WithRoles }
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import java._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by Igbalajobi Jamiu Okunade on 5/21/17.
 */
@Singleton
class HotelBookingCtrl @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi, implicit val encrypt: Encrypt, mailService: MailService, services: ServicesAPI, hotelCRUD: HotelCRUD) extends AdminController {

  def fromUrl(implicit requestHeader: RequestHeader) = requestHeader.headers.get(REFERER).getOrElse(controllers.admin.management.routes.HotelBookingCtrl.index().absoluteURL())

  def index(qType: String) = SecuredAction { implicit request =>
    val bookingLists = qType match {
      case "hotelQueues.awaitingAction" => HotelBookings.find.where().eq("bookings.isArchived", false).findList()
      case "hotelQueues.bookingByStatus.failed" => HotelBookings.find.where().eq("bookings.isArchived", false).where().eq("bookings.status", BookingStatus.FAILED_BOOKING).findList()
      case "hotelQueues.bookingByStatus.cancelled" => HotelBookings.find.where().eq("bookings.isArchived", false).where().eq("bookings.status", BookingStatus.CANCELLED).findList()
      case "hotelQueues.bookingByStatus.archived" => HotelBookings.find.where().eq("bookings.isArchived", true).findList()
      case _ => HotelBookings.find.all()
    }
    Ok(hotelBookings.index(bookingLists.asScala.toList))
  }

  def manage(ref: String) = SecuredAction.async { implicit request =>
    val hotelBooking = HotelBookings.find.where().eq("bookings.transactionRef", encrypt.decrypt(ref)).findUnique()
    //Log the user to this booking
    hotelBooking.getBookings.setAuthOperatorId(request.identity)
    hotelBooking.getBookings.setAuthIsReadable(true)
    hotelBooking.getBookings.setAuthIsWriteable(false)
    hotelBooking.getBookings.update()
    services.hotelApi.performHotelAvailability(hotelAvailibility(hotelBooking)).map { hotel =>
      Ok(hotelBookings.manage(hotelBooking, hotel))
    }
  }

  def queryBooking(hid: String) = SecuredAction.async { implicit request =>
    val hotelBookings = HotelBookings.find.byId(lang.Long.parseLong(encrypt.decrypt(hid)))
    for {
      apiQuery <- services.hotelApi.performHotelBookingQuery(hotelQuery(hotelBookings))
      _ <- hotelCRUD.update(apiQuery, hotelBookings, request.identity)
      response <- Future.successful(Redirect(fromUrl).withNewSession.flashing(("success", "Booking Cancelled Successfully")))
    } yield response
  }

  def cancelBooking(hid: String) = SecuredAction.async { implicit request =>
    val hotelBookings = HotelBookings.find.byId(lang.Long.parseLong(encrypt.decrypt(hid)))
    for {
      apiQuery <- services.hotelApi.performHotelBookingCancel(hotelCancel(hotelBookings))
      //TODO AuditTrail Action
      _ <- hotelCRUD.delete(hotelBookings, apiQuery, request.identity)
      response <- Future.successful(Redirect(fromUrl).withNewSession.flashing(("success", "Booking Cancelled Successfully")))
    } yield response
  }

  private def hotelQuery(hotelBookings: HotelBookings): QueryHotelRequest = {
    val queryBooking = new QueryHotelRequest
    //Log the Query
    queryBooking.setBookingreference(hotelBookings.getSupplierVoucherReference)
    queryBooking.setSupplierId(hotelBookings.getBookings.getSupplier.getName)
    queryBooking
  }

  private def hotelCancel(hotelBookings: HotelBookings): CancelHotelRequest = {
    val cancelHotelDto = new CancelHotelRequest
    val order = new Order
    val orderPrice = new OrderPrice
    order.setOrderPrice(orderPrice)
    order.setVoucherReferenceCode(hotelBookings.getSupplierVoucherReference)
    order.setSupplierId(hotelBookings.getBookings.getSupplier.getName)
    order.setSign("confirm")
    cancelHotelDto.setOrder(order)
    cancelHotelDto
  }

  def hotelAvailibility(hotelBookings: HotelBookings): HotelAvailabilityRequest = {
    val supplierConfigManager = SupplierConfigManager.getSupplierInfo(hotelBookings.getBookings.getSupplier)
    val availabilityRequest = new HotelAvailabilityRequest
    availabilityRequest.setCountryId(supplierConfigManager("supplier.country").configValue)
    availabilityRequest.setCityId(hotelBookings.getCityCode)
    //For 'HotelAvailability' API
    availabilityRequest.setHotelId(List[String](hotelBookings.getHotelCode).asJava)
    availabilityRequest.setDestinationType("city")
    availabilityRequest.setSupplierId(hotelBookings.getBookings.getSupplier.getName)
    val dateFormat = new SimpleDateFormat(supplierConfigManager("supplier.dateformat").configValue)
    availabilityRequest.setCheckIn(dateFormat.format(hotelBookings.getCheckIn))
    availabilityRequest.setCheckOut(dateFormat.format(hotelBookings.getCheckOut))
    availabilityRequest.setCurrency(supplierConfigManager("supplier.currency").configValue)
    availabilityRequest.setNationality(supplierConfigManager("supplier.nationality").configValue)
    var rooms = List[Room]()
    rooms ::= new Room("1", 1, CRoomtype.SINGLE, 1, 0, null)
    rooms ::= new Room("2", 1, CRoomtype.SINGLE, 2, 0, null)
    availabilityRequest.setRooms(rooms.asJava)
    availabilityRequest
  }

  def comment(bid: Long) = SecuredAction { implicit request =>
    val itinerary = models.Bookings.find.byId(bid)
    val comment = new BookingMsgLog
    comment.setAuthUserId(request.identity)
    comment.setBookingId(itinerary)
    comment.setMsg(request.body.asFormUrlEncoded.get("comment").head)
    comment.setActionType("user")
    comment.save()
    Redirect(fromUrl).withNewSession.flashing(("success", "Comment Saved Successfully"))
  }

  def bookingInvoice(itinerary: String) = SecuredAction { implicit request =>
    var redirectTo = fromUrl
    val (paymentStatus, ticketStatus) = (Option(request.body.asFormUrlEncoded.get("paymentStatus").head), Option(request.body.asFormUrlEncoded.get("ticketStatus").head))
    val booking = Bookings.find.byId(java.lang.Long.parseLong(encrypt.decrypt(itinerary)))
    booking.getPaymentHistoryId.setStatus(PaymentStatus.valueOf(paymentStatus.getOrElse(booking.getPaymentHistoryId.getStatus.name())))
    booking.getPaymentHistoryId.update()
    booking.setStatus(BookingStatus.valueOf(ticketStatus.getOrElse(booking.getStatus.name())))
    request.body.asFormUrlEncoded.get("action").head match {
      case "archieveBooking" =>
        booking.setAuthIsWriteable(true)
        booking.setAuthIsReadable(true)
        booking.setAuthOperatorId(null)
        redirectTo = routes.HotelBookingCtrl.index().absoluteURL().replaceAll("https:", "http:")
      case "sendBookingConfirmation" =>
      case "sendTicketEmail" =>
      case _ => booking.setId(java.lang.Long.parseLong(encrypt.decrypt(itinerary)))
    }
    booking.update()
    Redirect(redirectTo).withNewSession.flashing(("success", "Booking Updated Successfully."))
  }

}
