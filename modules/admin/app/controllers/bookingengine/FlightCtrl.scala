package controllers.admin.bookingengine

import java.net.URLEncoder
import javax.inject.Inject

import api.ServicesAPI
import com.avaje.ebean.{ Ebean, RawSqlBuilder }
import com.google.common.net.HttpHeaders
import com.mohiva.play.silhouette.api.Silhouette
import com.alajobi.ota.utils.CachedVariables
import com.alajobi.ota.flights.{ FlightCRUD, FlightTravellerInfo, FlightBookingForm, ResultSorting }
import crypto._
import flight.dto.entity.{ BookingRequest, FlightSearchResponse, PNRDetails, PNRModifyRequest }
import mailer.MailService
import models._
import play.api.i18n.MessagesApi
import play.api.libs.json.{ JsNumber, JsObject }
import play.api.mvc.{ Request, RequestHeader }
import utils.silhouette.MyEnv
import utils.silhouette.admin.{ AdminController, WithRoles }
import views.html.admin._
import views.html.admin.mails.{ flightInvoice, flightTicket }

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by
 * Igbalajobi Jamiu O. on 22/08/2016 10:32 PM.
 */
class FlightCtrl @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi, encrypt: Encrypt, services: ServicesAPI, mailService: MailService, flightCrud: FlightCRUD) extends AdminController with ResultSorting {

  def fromUrl(implicit requestHeader: RequestHeader) = requestHeader.headers.get(REFERER).getOrElse(controllers.admin.management.routes.FlightBookingCtrl.index().absoluteURL())

  implicit val ms = mailService

  val roles = WithRoles(Roles.sale_manager.name(), Roles.operation_manager.name(), Roles.agent_manager.name())

  implicit val enc = encrypt

  var travellerForm = FlightBookingForm.travellerForm
  var productPaymentForm = FlightBookingForm.productsPaymentForm

  private def getSearchResponse(implicit request: Request[_]): FlightSearchResponse = services.cache.getItem(CachedVariables.flightResultKey).orNull.asInstanceOf[FlightSearchResponse]

  def result(locale: String, fl_tabHash: String) = SecuredAction { implicit request =>
    if (getSearchResponse != null) {
      Ok(bookingengine.flights.result(getSearchResponse, byAirline = byAirline(getSearchResponse.getPricedItineraryWSResponses.asScala.toList)))
    } else
      Redirect(request.headers.get(HttpHeaders.FROM).getOrElse("/")).withNewSession.flashing(("error", "Sorry, Session Expired. Please try Again"))
  }

  def detail(itineraryRef: String, fl_tabHash: String) = SecuredAction { implicit request =>
    if (getSearchResponse != null) {
      Ok(bookingengine.flights.detail(getSearchResponse.getPricedItineraryWSResponses.find(_.getCacheIndex == itineraryRef.toInt).get, getSearchResponse.getFlightSearchRequest, travellerForm))
    } else {
      Redirect(request.headers.get(HttpHeaders.FROM).getOrElse("/")).withNewSession.flashing(("error", "Sorry, Session Expired. Please try Again"))
    }
  }

  def submitTraveller(itineraryRef: String, fl_tabHash: String) = SecuredAction { implicit request =>
    travellerForm.bindFromRequest().fold(
      error => BadRequest(bookingengine.flights.detail(getSearchResponse.getPricedItineraryWSResponses.find(_.getCacheIndex == itineraryRef.toInt).get, getSearchResponse.getFlightSearchRequest, error)),
      success => {
        if (getSearchResponse != null) {
          services.cache.setItem(s"bookingRequest_$fl_tabHash", success)
          Redirect(controllers.admin.bookingengine.routes.FlightCtrl.bookingCheckingOut(itineraryRef, fl_tabHash))
        } else {
          Redirect(request.headers.get(HttpHeaders.FROM).getOrElse("/")).withNewSession.flashing(("error", "Sorry, Session Expired. Please try Again"))
        }
      }
    )
  }

  def bookingCheckingOut(itineraryRef: String, fl_tabHash: String) = SecuredAction { implicit request =>
    services.cache.getItem[FlightSearchResponse](CachedVariables.flightResultKey) match {
      case searchResponse: Some[FlightSearchResponse] =>
        val selectedItinerary = searchResponse.get.getPricedItineraryWSResponses.find(_.getCacheIndex == itineraryRef.toInt).get
        Ok(bookingengine.flights.productCheckout(selectedItinerary, searchResponse.get.getFlightSearchRequest, productPaymentForm, encrypt = encrypt))
      case _ => Redirect(request.headers.get(HttpHeaders.FROM).getOrElse("/")).withNewSession.flashing(("error", "Sorry, Session Expired. Please try Again"))
    }
  }

  def bookItinerary(itineraryRef: String, fl_tabHash: String) = SecuredAction.async { implicit request =>
    val selectedItinerary = getSearchResponse.getPricedItineraryWSResponses.find(_.getCacheIndex == itineraryRef.toInt).get
    productPaymentForm.bindFromRequest().fold(
      error => Future(BadRequest(bookingengine.flights.productCheckout(selectedItinerary, getSearchResponse.getFlightSearchRequest, error, encrypt = encrypt))),
      productPaymentPayment => {
        val travellerInfoForm = services.cache.getItem(s"bookingRequest_$fl_tabHash").asInstanceOf[Option[FlightTravellerInfo]].get
        val bookingRequest = new BookingRequest
        bookingRequest.setPaymentMethod(productPaymentPayment.paymentMethodId)
        bookingRequest.setItineraryWSResponse(selectedItinerary)
        bookingRequest.setSupplier(selectedItinerary.getSupplier)
        bookingRequest.getPassengers.addAll(FlightBookingForm.getFlightPassenger(travellerInfoForm))
        for {
          pnr <- services.flightApi.createPNR(bookingRequest)
          booking <- flightCrud.create(pnr, bookingRequest, None, travellerInfoForm.remarks, request.identity.asInstanceOf[PrivateUsers])
          _ <- Future {
            services.cache.remove(s"bookingRequest_$fl_tabHash")
            services.cache.remove(CachedVariables.flightResultKey)
          }
          _ <- Future(if (pnr.getStatus.equals(BookingStatus.CONFIRMED)) utils.Mailer.flightBooking(pnr, booking, selectedItinerary) else utils.Mailer.failedBooking(booking, selectedItinerary))
        } yield Redirect(controllers.admin.bookingengine.routes.FlightCtrl.complete(pnr.getStatus.name(), booking.getId))
      }
    )
  }

  def complete(status: String, bookingId: Long) = SecuredAction { implicit request =>
    val booking = Bookings.find.byId(bookingId)
    if (booking.getStatus.equals(BookingStatus.FAILED) || booking.getStatus.equals(BookingStatus.FAILED_BOOKING)) {
      Redirect(controllers.admin.routes.ApplicationCtrl.index()).flashing(("error", "Booking failed"))
    } else {
      Redirect(controllers.admin.routes.ApplicationCtrl.index()).flashing(("success", "Booking Successful."))

    }
  }
}