package controllers.admin.management

import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject

import api.ServicesAPI
import com.avaje.ebean.{ Ebean, Expr, RawSqlBuilder }
import com.mohiva.play.silhouette.api.Silhouette
import flight.dto.entity.{ PNRDetails, PNRModifyRequest }
import crypto._
import models._
import org.apache.commons.lang3.text.WordUtils
import play.api.i18n.MessagesApi
import play.api.libs.json.{ JsNumber, JsObject }
import mailer.MailService
import utils.silhouette.MyEnv

import models.admin.MailTokenManager
import utils.silhouette.admin.{ AdminController, ManagerService, WithRole, WithRoles }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import views.html.admin._
import play.api.mvc.RequestHeader
import views.html.admin.mails.{ flightInvoice, flightTicket }
import play.api.data.Form
import play.api.data.Forms._

/**
 * Created by
 * Igbalajobi Jamiu O. on 22/08/2016 10:32 PM.
 */
class FlightBookingCtrl @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi, encrypt: Encrypt, services: ServicesAPI, mailService: MailService) extends AdminController {

  def fromUrl(implicit requestHeader: RequestHeader) = requestHeader.headers.get(REFERER).getOrElse(controllers.admin.management.routes.FlightBookingCtrl.index().absoluteURL())

  implicit val ms = mailService

  val roles = WithRoles(Roles.sale_manager.name(), Roles.operation_manager.name(), Roles.agent_manager.name())
  implicit val enc = encrypt

  val commentForm = Form(tuple(
    "comment" -> nonEmptyText,
    "itineraryId" -> nonEmptyText
  ))

  def ticketingQueue = SecuredAction { implicit request =>
    val ticketingList = BookingQueues.find.where().eq("actionType", "AWAITING_ISSUE").findList()
    Ok(flightBookings.ticketingQueue(ticketingList.asScala.toList))
  }

  def removeQueue(id: Long) = SecuredAction { implicit request =>
    BookingQueues.find.byId(id).delete()
    Redirect(fromUrl).withNewSession.flashing(("success", "Queue Removed Successfully"))
  }

  def index(qType: String) = SecuredAction { implicit request =>
    val flightList = qType match {
      case "flightQueues.awaitingAction" => FlightBookings.find.where().eq("bookings.isArchived", false).findList()
      case "flightQueues.bookingByStatus.issued" => FlightBookings.find.where().eq("bookings.status", BookingStatus.TICKET_ISSUED).findList()
      case "flightQueues.bookingByStatus.cancelled" => FlightBookings.find.where().eq("bookings.status", BookingStatus.CANCELLED).findList()
      case "flightQueues.bookingByStatus.failed" => FlightBookings.find.where().eq("bookings.status", BookingStatus.FAILED_BOOKING).findList()
      case "flightQueues.b2bSales.all" => FlightBookings.find.where().eq("bookings.isArchived", false).where().eq("bookings.salesCategory", SalesCategory.B2B).findList()
      case "flightQueues.b2bSales.failed" => FlightBookings.find.where().eq("bookings.isArchived", false).where().eq("bookings.salesCategory", SalesCategory.B2B).where().eq("bookings.status", BookingStatus.FAILED_BOOKING).findList()
      case "flightQueues.b2bSales.cancelled" => FlightBookings.find.where().eq("bookings.isArchived", false).where().eq("bookings.salesCategory", SalesCategory.B2B).where().eq("bookings.status", BookingStatus.CANCELLED).findList()
      case "flightQueues.b2bSales.paymentSuccess" => FlightBookings.find.where().eq("bookings.isArchived", false).where().eq("bookings.salesCategory", SalesCategory.B2B).where().eq("bookings.paymentHistoryId.status", PaymentStatus.Paid).findList()
      case "flightQueues.b2bSales.paymentFailed" => FlightBookings.find.where().eq("bookings.isArchived", false).where().eq("bookings.salesCategory", SalesCategory.B2B).where().eq("bookings.paymentHistoryId.status", PaymentStatus.Failed).findList()
      case "flightQueues.paymentStatus.confirmed" => FlightBookings.find.where().eq("bookings.isArchived", false).where().eq("bookings.isPaymentConfirmed", true).findList()
      case "flightQueues.paymentMethod.webpay" => FlightBookings.find.where().eq("bookings.isArchived", false).where().in("bookings.paymentHistoryId.paymentMethodId.paymentCategory", PaymentCategories.Debit_Card, PaymentCategories.Credit_Card).where().eq("paymentHistoryId.status", PaymentStatus.Paid).where().eq("bookings.paymentHistoryId.status", PaymentStatus.Paid).findList()
      case "flightQueues.flightQueues.webpay.failed" => FlightBookings.find.where().eq("isArchived", false).where().in("bookings.paymentHistoryId.paymentMethodId.paymentCategory", PaymentCategories.Credit_Card, PaymentCategories.Debit_Card).where().eq("bookings.paymentHistoryId.status", PaymentStatus.Failed).findList()
      case "flightQueues.paymentMethod.webpay.debit" => FlightBookings.find.where().eq("bookings.isArchived", false).where().eq("bookings.paymentHistoryId.paymentMethodId.paymentCategory", PaymentCategories.Debit_Card).findList()
      case "flightQueues.paymentMethod.webpay.credit" => FlightBookings.find.where().eq("bookings.isArchived", false).where().eq("bookings.paymentHistoryId.paymentMethodId.paymentCategory", PaymentCategories.Credit_Card).findList()
      case "flightQueues.paymentMethod.bookOnHold" => FlightBookings.find.where().eq("bookings.isArchived", false).where().eq("bookings.paymentHistoryId.paymentMethodId.code", PaymentCategories.CASH_PAYMENT).findList()
      case "flightQueues.paymentMethod.webpay.success" => FlightBookings.find.where().eq("bookings.isArchived", false).where().eq("bookings.paymentHistoryId.paymentMethodId.code", PaymentCategories.Debit_Card).where().eq("bookings.paymentHistoryId.status", PaymentStatus.Paid).findList()
      case "flightQueues.flightQueues.paymentFailed" => FlightBookings.find.where().eq("bookings.isArchived", false).where().eq("bookings.paymentHistoryId.paymentMethodId.code", PaymentCategories.Debit_Card).where().eq("bookings.paymentHistoryId.status", PaymentStatus.Failed).findList()
      case _ => FlightBookings.find.all()
    }
    Ok(flightBookings.bookingList("Booking Queues", flightList.asScala.toList))
  }

  def itinerary(ref: String) = SecuredAction { implicit request =>
    val itinerary = FlightBookings.find.where().eq("id", encrypt.decrypt(ref)).findUnique()
    if (itinerary.getBookings.getAuthOperatorId == null) {
      val msgTitle = s"${request.identity.fullName()} StaffID #${request.identity.getStaffRef} locked # ${itinerary.getBookings.getTransactionRef}"
      //Let the user be assigned to the ticket.
      val flightBooking = FlightBookings.find.byId(java.lang.Long.parseLong(itinerary.getId.toString))
      flightBooking.getBookings.setAuthOperatorId(request.identity)
      flightBooking.getBookings.setAuthIsReadable(true)
      flightBooking.getBookings.setAuthIsWriteable(false)
      flightBooking.getBookings.update()
      flightBooking.update()

      val msgLog = new BookingMsgLog
      msgLog.setBookingId(flightBooking.getBookings)
      msgLog.setAuthUserId(request.identity)
      msgLog.setMsg(msgTitle)
      msgLog.setActionType("user")
      msgLog.insert()
      //save the information in the message log
      //      AuditResponseDto(Some(request.identity), s"${itinerary.transactionRef} Locked", msgTitle, "flight_bookings", itinerary.id.toString, getModule("itinerary").orNull)
    }
    Ok(flightBookings.flightItinerary(itinerary, commentForm))
  }

  def sendMailSms(itineraryId: Long) = SecuredAction { implicit request =>
    val from = request.body.asFormUrlEncoded.get("from").head
    val mtype = request.body.asFormUrlEncoded.get("type").head
    val to = request.body.asFormUrlEncoded.get("recipient").head
    val subject = request.body.asFormUrlEncoded.get("subject").head
    val message = request.body.asFormUrlEncoded.get("emailContent").head
    ms.sendEmailAsync(to)(subject, message, message)
    val msgLogs = new BookingMsgLog
    msgLogs.setAuthUserId(request.identity)
    msgLogs.setActionType("user")
    msgLogs.setBookingId(FlightBookings.find.byId(itineraryId).getBookings)
    msgLogs.setTitle(subject)
    msgLogs.setMsg(message)
    msgLogs.insert()
    //Email sent successfully
    val msg = s"${request.identity.fullName()} Sent an $mtype to $to Booking RefID is ${msgLogs.getBookingId.getFlightBookings.getBookings.getTransactionRef}"
    //TODO -> Audit log
    //    env.audit.log { a =>
    //      //save the information in the message log
    //      AuditResponseDto(Some(request.identity), s"sent $mtype", msg, "flight_bookings", msgLogs.flightBookingId.id.toString, getModule("sendMailSms").orNull)
    //    }
    Ok(JsObject(Map(
      "responseCode" -> JsNumber(200)
    )))
  }

  def comment(itineraryId: Long) = SecuredAction { implicit request =>
    commentForm.bindFromRequest.fold(
      error => {
        println("VALUE" + error.value)
        error.errors.foreach(a => println("Error: " + a.message, ": Key = " + a.key))
        Redirect(fromUrl).withNewSession.flashing(("error", "Comment could not be saved"))
      },
      success => {
        println(success._1)
        val itinerary = FlightBookings.find.byId(itineraryId)
        val comment = new BookingMsgLog
        comment.setAuthUserId(request.identity)
        comment.setBookingId(itinerary.getBookings)
        comment.setMsg(success._1)
        comment.setActionType("user")
        comment.insert()
        Redirect(fromUrl).withNewSession.flashing(("success", "Comment Saved Successfully"))
      }
    )
  }

  def previewComment(commentId: Long) = SecuredAction { implicit request =>
    Ok(flightBookings.previewComment(BookingMsgLog.find.byId(commentId)))
  }

  // (WithRoles(Roles.operation_manager.name(), Roles.admin.name(), Roles.sale_manager.name())) 
  def addItineraryToQueue(itineraryId: String) = SecuredAction { implicit request =>
    val itinerary = Bookings.find.byId(java.lang.Long.parseLong(enc.decrypt(itineraryId)))
    val flightQueue = new BookingQueues
    flightQueue.setBookingId(itinerary)
    flightQueue.setAuthUserId(request.identity)
    val queueType = request.body.asFormUrlEncoded.get("queueType").head
    flightQueue.setActionType(queueType)
    flightQueue.setStatus(models.Status.Active)
    flightQueue.save()
    //    flightQueue.setActionType("user")
    //    flightQueue.queueType = queueType
    //    flightQueue.setComment(s"""RefID #${itinerary.getTransactionRef} add to $queueType""")
    //    flightQueue.insert()
    Redirect(fromUrl).withNewSession.flashing(("success", "Itinerary added to queued successfully"))
  }

  def modifyQueue = SecuredAction { implicit request =>
    //    val flightQueue = BookingQueues.find.byId(java.lang.Long.parseLong(request.getQueryString("queueId").orNull))
    //    flightQueue.flightBookingId.id = flightQueue.flightBookingId.id
    //    flightQueue.flightBookingId.updatedAt = new Date()
    //    flightQueue.flightBookingId.update()
    val action = request.getQueryString("action").get
    //    action match {
    //      case "save" if request.getQueryString("queueId").isDefined =>
    ////        val flightQueues = BookingQueues.find.byId(java.lang.Long.parseLong(request.getQueryString("queueId").get))
    ////        flightQueues.setComment(request.getQueryString("comment").get)
    ////        flightQueues.update()
    //      case "delete" if request.getQueryString("queueId").isDefined => BookingQueues.find.byId(java.lang.Long.parseLong(request.getQueryString("queueId").get)).delete()
    //    }
    Redirect(fromUrl).withNewSession.flashing(("success", s"Queue $action successfully"))
  }

  //TODO
  def vendorApiAction(actionType: String, isAuthorized: String, itinerary: String) = SecuredAction.async { implicit request =>
    var responsetext = "Request failed, please try again"
    val flightBookings = FlightBookings.find.byId(java.lang.Long.parseLong(enc.decrypt(itinerary)))
    (actionType match {
      case "cancel" if !flightBookings.getBookings.getStatus.eq(BookingStatus.CANCELLED) =>
        val cancel = services.flightApi.cancelPNR(PNRModifyRequest(pnrRef = flightBookings.getPnrRef, supplier = flightBookings.getBookings.getSupplier.getName, surname = flightBookings.getBookings.getContactSurname))
        flightBookings.getBookings.setStatus(BookingStatus.CANCELLED)
        flightBookings.getBookings.update()
        flightBookings.update()
        utils.Mailer.cancelBooking(flightBookings)
        cancel
      case _ => Future.successful(new PNRDetails())
    }).map { pnrDetail: PNRDetails =>
      if (pnrDetail.getStatus != null) {
        if (actionType.equalsIgnoreCase("cancel")) {
          responsetext = "Booking Cancelled Successfully."
        } else if (actionType.equalsIgnoreCase("refresh")) {
          //refresh the PNR from the WS and Update the Itinerary.
          responsetext = "Itinerary Refresh and Updated Successfully"
        } else if (actionType.equalsIgnoreCase("reprice")) {
          //Update the GDS Fare in flightBooking tbl.
          val priceInfo = pnrDetail.getBookingRequest.getItineraryWSResponse.getPricingInfoWSResponse
          val repriceLog = new FlightBookingRepriceLog
          repriceLog.setCurrency(priceInfo.getBaseFare.getCurrencyCode)
          repriceLog.setBaseFare(priceInfo.getBaseFare.getAmount)
          repriceLog.setTax(priceInfo.getTotalTax.asScala.map(_.getAmount.doubleValue()).sum)
          repriceLog.setTotalFare(priceInfo.getTotalFare)
          repriceLog.setAuthUserId(request.identity)
          repriceLog.setFlightBookingId(flightBookings)
          repriceLog.setEquivBaseFare(priceInfo.getGdsEquivBaseFare)
          repriceLog.setEquivFareCurrency(priceInfo.getGdsEquivCurrency)
          repriceLog.save()
          responsetext = "Reprice Successful. Check `GDS Re-Price Logs` Section Below"
        }
        //        env.audit.log { a =>
        //
        //          AuditResponseDto(Some(request.identity), title, msg, "flight_bookings", flightBookings.id.toString, getModule("vendorApiAction").orNull)
        //        }
      } else {
        pnrDetail.setStatus(BookingStatus.FAILED)
        responsetext = "Booking Cancelled Successfully."
      }
      Redirect(fromUrl).withNewSession.flashing((if (pnrDetail.getStatus.eq(BookingStatus.FAILED)) "failed" else "success", responsetext))
    }
  }

  def bookingInvoice(itinerary: String) = SecuredAction { implicit request =>
    var redirectTo = fromUrl
    val (paymentStatus, ticketStatus, ticketingPartnerId) = (Option(request.body.asFormUrlEncoded.get("paymentStatus").head), Option(request.body.asFormUrlEncoded.get("ticketStatus").head), Option(request.body.asFormUrlEncoded.get("ticketingPartnerId").head))
    val flightBookings = FlightBookings.find.byId(java.lang.Long.parseLong(enc.decrypt(itinerary)))
    flightBookings.getBookings.getPaymentHistoryId.setStatus(PaymentStatus.valueOf(paymentStatus.getOrElse(flightBookings.getBookings.getPaymentHistoryId.getStatus.name())))
    flightBookings.getBookings.getPaymentHistoryId.update()
    flightBookings.getBookings.setStatus(BookingStatus.valueOf(ticketStatus.getOrElse(flightBookings.getBookings.getStatus.name())))
    if (ticketingPartnerId.isDefined && ticketingPartnerId.get.nonEmpty) {
      flightBookings.setTicketingPartner(TicketingPartners.find.byId(java.lang.Long.parseLong(ticketingPartnerId.get)))
    }
    flightBookings.getPnrRecords.asScala.toList.foreach { passenger =>
      passenger.setTicketRef(request.body.asFormUrlEncoded.get(s"gdsTicketId_${passenger.getId}").head)
      passenger.setId(java.lang.Long.parseLong(request.body.asFormUrlEncoded.get(s"pass_${passenger.getId}").head))
      passenger.update()
    }
    request.body.asFormUrlEncoded.get("action").head match {
      case "archieveBooking" =>
        flightBookings.getBookings.setAuthOperatorId(null)
        flightBookings.getBookings.setAuthIsWriteable(true)
        flightBookings.getBookings.setAuthIsReadable(true)
        flightBookings.getBookings.setArchived(true)
        flightBookings.getBookings.update()
        redirectTo = routes.FlightBookingCtrl.index("flightQueues.awaitingAction").absoluteURL().replaceAll("https:", "http:")
      case "sendTicketEmail" =>
        val ticketInvoice = flightTicket(flightBooking = flightBookings).body
        mailService.sendEmailAsync(request.body.asFormUrlEncoded.get("ticketRecipientEmail").head)(s"${
          flightBookings.getBookings.getSalesCategory match {
            case SalesCategory.B2B => flightBookings.getBookings.getUserId.getAgentCorporateDetailId.getCompanyName
            case SalesCategory.B2C => play.Configuration.root().getString("project.name")
          }
        }  eTicket (${flightBookings.getBookings.getTransactionRef})", ticketInvoice, ticketInvoice)
      case "sendBookingConfirmation" =>
        val bookingInvoice = flightInvoice(flightBookings = flightBookings).body
        mailService.sendEmailAsync(request.body.asFormUrlEncoded.get("ticketRecipientEmail").head)(s"${
          flightBookings.getBookings.getSalesCategory match {
            case SalesCategory.B2B => flightBookings.getBookings.getUserId.getAgentCorporateDetailId.getCompanyName
            case SalesCategory.B2C => play.Configuration.root().getString("project.name")
          }
        } Booking Confirmation (${flightBookings.getBookings.getTransactionRef})", bookingInvoice, bookingInvoice)
      case _ => flightBookings.setId(java.lang.Long.parseLong(enc.decrypt(itinerary)))
    }
    flightBookings.getBookings.update()
    flightBookings.update()
    Redirect(redirectTo).withNewSession.flashing(("success", "Booking Updated Successfully."))
  }

  def enquiry(transactionRef: String) = SecuredAction { implicit request =>
    Ok("")
  }

  def payOnline(transactionRef: String) = SecuredAction { implicit request =>
    Ok("")
  }

  def unlockAccess(itinerary: String) = SecuredAction { implicit request =>
    val booking = FlightBookings.find.byId(java.lang.Long.parseLong(enc.decrypt(itinerary)))
    //TODO -> Audit log
    //    env.audit.log { a =>
    //      val msgTitle = s"${request.identity.fullName()} StaffID #${request.identity.staffRef} Release Access to Itinerary PNR ${booking.transactionRef}"
    //      //Let the user be assigned to the ticket.
    //      booking.authOperatorId = null
    //      booking.update()
    //      //save the information in the message log
    //      AuditResponseDto(Some(request.identity), s"${booking.transactionRef} Access Released", msgTitle, "flight_bookings", booking.id.toString, getModule("unlockAccess").orNull)
    //    }
    Redirect(routes.FlightBookingCtrl.index()).withNewSession.flashing(("success", "Itinerary Unlocked Successfully."))
  }

}