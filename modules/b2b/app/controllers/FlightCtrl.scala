package controllers.b2b

import java.net.URLDecoder
import java.util
import javax.inject.{ Inject, Singleton }

import api.ServicesAPI
import caching.CacheApi
import com.google.common.net.HttpHeaders
import com.mohiva.play.silhouette.api.Silhouette
import com.alajobi.ota.utils.CachedVariables
import crypto.Encrypt
import flight.dto.entity.{ BookingRequest, FlightSearchRequest, FlightSearchResponse, ItineraryWSResponse }
import com._
import models.AppFeatureLibraries._
import models._
import play.api.i18n.{ Lang, MessagesApi }
import play.api.libs.json.{ JsNumber, JsObject, JsString }
import play.api.mvc._
import mailer.MailService
import utils.b2b.Mailer
import utils.silhouette._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import views.html.b2b._
import com.alajobi.ota.flights._
import play.cache.Cached
import views.html.b2b.flight
import utils.silhouette.b2b._

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._

@Singleton
class FlightCtrl @Inject() (val silhouette: Silhouette[MyEnv[Users]], serviceApi: ServicesAPI, flightCrud: FlightCRUD, val messagesApi: MessagesApi, implicit val encrypt: Encrypt, val mailService: MailService) extends B2BController with ResultSorting {
  implicit val cacheApi = serviceApi.cache
  implicit val ms = mailService
  var form = FlightBookingForm.travellerForm
  var formCheckout = FlightBookingForm.productsPaymentForm

  private def getSearchResponse(cacheIndex: Option[String] = None)(implicit request: Request[_]): FlightSearchResponse = cacheApi.getItem[FlightSearchResponse](CachedVariables.flightResultKey) match {
    case items: Some[FlightSearchResponse] if cacheIndex isDefined =>
      items.get.setPricedItineraryWSResponses(items.get.getPricedItineraryWSResponses.filter(_.getCacheIndex.equals(cacheIndex.get.toInt)).asJava); items.get
    case items: Some[FlightSearchResponse] => items.get
    case _ => null
  }

  @Cached(key = "b2b.flight")
  def index = SecuredAction.async { implicit request =>
    Future.successful(Ok(flight.index()))
  }

  def result(region: String) = SecuredAction { implicit request =>
    if (getSearchResponse() != null) {
      Ok(flight.result(getSearchResponse(), byAirline = byAirline(getSearchResponse().getPricedItineraryWSResponses.asScala.toList), byDates(getSearchResponse().getPricedItineraryWSResponses.asScala.toList)))
    } else {
      Redirect(request.headers.get(HttpHeaders.FROM).getOrElse("/search-for-flight")).withNewSession.flashing(("error", "Sorry, Session Expired. Please try Again"))
    }
  }

  def detail(itineraryRef: String, fl_tabHash: String) = SecuredAction { implicit request =>
    if (getSearchResponse() != null) {
      Ok(flight.detail(itineraryRef, fl_tabHash, getSearchResponse().getPricedItineraryWSResponses.find(_.getCacheIndex == itineraryRef.toInt).get, getSearchResponse().getFlightSearchRequest, form))
    } else {
      Redirect(request.headers.get(HttpHeaders.FROM).getOrElse("/search-for-flight")).withNewSession.flashing(("error", "Sorry, Session Expired. Please try Again"))
    }
  }

  def submitTraveller(itineraryRef: String, fl_tabHash: String) = SecuredAction { implicit request =>
    form.bindFromRequest().fold(
      error => BadRequest(flight.detail(itineraryRef, fl_tabHash, getSearchResponse().getPricedItineraryWSResponses.find(_.getCacheIndex == itineraryRef.toInt).get, getSearchResponse().getFlightSearchRequest, error)),
      success => {
        if (getSearchResponse() != null) {
          cacheApi.setItem(s"bookingRequest_$fl_tabHash", success)
          Redirect(routes.FlightCtrl.bookingCheckingOut(itineraryRef, fl_tabHash))
        } else {
          Redirect(request.headers.get(HttpHeaders.FROM).getOrElse("/flight/search")).withNewSession.flashing(("error", "Sorry, Session Expired. Please try Again"))
        }
      }
    )
  }

  def bookingCheckingOut(itineraryRef: String, fl_tabHash: String) = SecuredAction { implicit request =>
    cacheApi.getItem[FlightSearchResponse](CachedVariables.flightResultKey) match {
      case searchResponse: Some[FlightSearchResponse] =>
        val selectedItinerary = searchResponse.get.getPricedItineraryWSResponses.find(_.getCacheIndex == itineraryRef.toInt).get
        val productOptions = ProductQuery.getProducts(searchResponse.get.getFlightSearchRequest)
        val hepstarPolicy = try {
          serviceApi.queryHepstarPolicies(searchResponse.get.getFlightSearchRequest, cacheApi.getItem(s"bookingRequest_$fl_tabHash").asInstanceOf[Option[BookingRequest]].get)
        } catch {
          case e: Throwable => null
        }
        Ok(flight.checkout(formCheckout, itineraryRef, fl_tabHash, selectedItinerary, searchResponse.get.getFlightSearchRequest, productOptions, hepstarPolicy, PaymentMethods.find.all().asScala.toList))
      case _ => Redirect(request.request.uri)
    }
  }

  def bookItinerary(itineraryRef: String, fl_tabHash: String) = SecuredAction.async { implicit request =>
    formCheckout.bindFromRequest().fold(
      error => Future {
        val selectedItinerary = getSearchResponse(Some(itineraryRef))
        val productOptions = ProductQuery.getProducts(selectedItinerary.getFlightSearchRequest)
        val hepstarPolicy = try {
          serviceApi.queryHepstarPolicies(selectedItinerary.getFlightSearchRequest, cacheApi.getItem(s"bookingRequest_$fl_tabHash").asInstanceOf[Option[BookingRequest]].get)
        } catch {
          case e: Throwable => null
        }
        Ok(flight.checkout(error, itineraryRef, fl_tabHash, selectedItinerary.getPricedItineraryWSResponses.head, selectedItinerary.getFlightSearchRequest, productOptions, hepstarPolicy, PaymentMethods.find.all().asScala.toList))
      },
      productPaymentPayment => {
        val travellerInfoForm = cacheApi.getItem[FlightTravellerInfo](s"bookingRequest_$fl_tabHash").get
        val selectedItinerary = getSearchResponse(Some(itineraryRef))
        val bookingRequest = new BookingRequest
        bookingRequest.setPaymentMethod(productPaymentPayment.paymentMethodId)
        if (request.body.asFormUrlEncoded.get.exists(a => a._1.equals("product_id[]"))) {
          bookingRequest.getProducts.addAll(request.body.asFormUrlEncoded.get("product_id[]").map { p =>
            val addOnProduct = AddonProducts.find.byId(java.lang.Long.parseLong(p))
            val bookingProduct = new BookingProducts
            bookingProduct.setAddonProductOptions(addOnProduct)
            val searchRequest = selectedItinerary.getFlightSearchRequest
            val productAmt = BigDecimal((searchRequest.getAdultCount * addOnProduct.getAdultPrice) + (searchRequest.getChildCount * addOnProduct.getChildPrice) + (searchRequest.getInfantCount * addOnProduct.getInfantPrice))
            bookingProduct.setAmount(productAmt.doubleValue())
            bookingProduct
          })
        }
        bookingRequest.setIncludeSMS(false)
        bookingRequest.setItineraryWSResponse(selectedItinerary.getPricedItineraryWSResponses.head)
        bookingRequest.setSupplier(selectedItinerary.getFlightSearchRequest.getSupplier)
        bookingRequest.getPassengers.addAll(FlightBookingForm.getFlightPassenger(travellerInfoForm))
        for {
          pnr <- serviceApi.flightApi.createPNR(bookingRequest)
          booking <- flightCrud.create(pnr, bookingRequest, Option(request.identity.asInstanceOf[Users]), travellerInfoForm.remarks)
          _ <- Future {
            cacheApi.remove(s"bookingRequest_$fl_tabHash")
            cacheApi.remove(CachedVariables.flightResultKey)
          }
          _ <- Future {
            if (pnr.getStatus.equals(BookingStatus.CONFIRMED)) {
              utils.Mailer.flightBooking(pnr, booking, selectedItinerary.getPricedItineraryWSResponses.head)
            } else
              utils.Mailer.failedBooking(booking, selectedItinerary.getPricedItineraryWSResponses.head)
          }
          result <- Future.successful(Redirect(routes.FlightCtrl.confirm(pnr.getStatus.name.toLowerCase(), encrypt.encrypt(booking.getId.toString))))
        } yield result
      }
    )
  }

  def noResult(statusCode: String) = SecuredAction { implicit request =>
    Ok(flight.resultError(FlightSearchStatusConverter.convert(statusCode)))
  }

  def confirm(bookingStatus: String, bookingId: String) = SecuredAction { implicit request =>
    Option(Bookings.find.byId(java.lang.Long.parseLong(URLDecoder.decode(encrypt.decrypt(bookingId))))) match {
      case bookings: Some[Bookings] =>
        if (bookings.get.getStatus.eq(BookingStatus.CONFIRMED)) {
          Ok(flight.bookingComplete(bookings.get.getFlightBookings))
        } else {
          Ok(flight.bookingFailed(bookings.get.getFlightBookings))
        }
      case _ => Ok("Invalid Booking Reference")
    }

  }

  def viewItineraryModal(ref: String, displayFare: Boolean) = SecuredAction.async { implicit request =>
    Future.successful {
      CachedVariables.getItinerary(ref.toInt) match {
        case i: Some[ItineraryWSResponse] => Ok(flight.viewItineraryModal(i.get, displayFare))
        case None => Ok("No Item Exist")
      }
    }
  }

  def sendInvoiceToMail(bookingId: Long) = SecuredAction { implicit request =>
    Mailer.flightBooking(Bookings.find.byId(bookingId).getFlightBookings, new ItineraryWSResponse(), request.getQueryString("email").get)
    Redirect(routes.ApplicationCtrl.report()).withNewSession.flashing(("success", "Ticket Mail Sent Successfully."))
  }

}