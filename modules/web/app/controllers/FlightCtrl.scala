package controllers.web

import java.net.{ URLEncoder, URLDecoder }
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util
import java.util.regex.Pattern
import java.util.{ Calendar, Date }

import com.google.common.net.HttpHeaders
import com.alajobi.ota.flights._
import flight.dto.entity._
import mailer.MailService
import play.api.i18n.Messages.Message
import utils.silhouette.{ MyEnv, WebController }
import play.api.mvc._
import play.api.i18n.MessagesApi

import scala.concurrent.Future
import javax.inject.{ Inject, Singleton }

import api.ServicesAPI
import com.mohiva.play.silhouette.api.Silhouette
import com.alajobi.ota.utils.CachedVariables
import com.alajobi.ota.flights._
import crypto.{ TransRefAlgorithm, Encrypt }
import models.SalesCategory

import scala.concurrent.ExecutionContext.Implicits.global
import views.html.web._
import models._

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._

@Singleton
class FlightCtrl @Inject() (val silhouette: Silhouette[MyEnv[Users]], val messagesApi: MessagesApi, serviceAPI: ServicesAPI, implicit val mailService: MailService, flightCrud: FlightCRUD, implicit val encrypt: Encrypt) extends WebController with ResultSorting {

  val travellerForm = FlightBookingForm.travellerForm

  val productsPaymentForm = FlightBookingForm.productsPaymentForm

  implicit val cacheApi = serviceAPI.cache

  def noResult(statusCode: String) = UserAwareAction { implicit request =>
    Ok(flights.noResult())
  }

  def result(region: String, fl_tabHash: String) = UserAwareAction { implicit request =>
    cacheApi.getItem[FlightSearchResponse](CachedVariables.flightResultKey) match {
      case None => Redirect(controllers.web.routes.FlightCtrl.noResult())
      case a @ Some(_) =>
        var result = a.get.getPricedItineraryWSResponses
        if (request.getQueryString("airlineCnt").isDefined) {
          var airlineCodes = List.empty[String]
          (0 until request.getQueryString("airlineCnt").get.toInt).foreach { index =>
            airlineCodes ::= request.getQueryString("airline[" + index + "]").get
          }
          result = result.filter(a => airlineCodes.contains(a.getAirlineCode))
        }
        if (request.getQueryString("stopCont").isDefined) {
          var stops = List.empty[String]
          (0 until request.getQueryString("stopCont").get.toInt).foreach { index =>
            stops ::= request.getQueryString("stop[" + index + "]").get
          }
          result = result.filter(a => stops.contains(a.getAirItineraryWSResponse.getOriginDestinationWSResponses.head.getNumberOfStops.toString))
        }
        if (request.getQueryString("cabinCnt").isDefined) {
          var cabin = List.empty[String]
          (0 until request.getQueryString("cabinCnt").get.toInt).foreach { index =>
            cabin ::= request.getQueryString("cabin[" + index + "]").get
          }
          result = result.filter(a => cabin.contains(a.getAirItineraryWSResponse.getOriginDestinationWSResponses.head.getCabin))
        }
        if (request.getQueryString("amtCnt").isDefined) {
          var amount = List.empty[String]
          (0 until request.getQueryString("amtCnt").get.toInt).foreach { index =>
            amount ::= request.getQueryString("amt[" + index + "]").get
          }
          result = result.filter(a => amount.exists(c => c.toDouble <= a.getPricingInfoWSResponse.getTotalFare))
        }

        Ok(flights.result(a.get, byAirline(result.asScala.toList), byDates(result.asScala.toList)))
    }
  }

  def detail(ref: String, fl_tabHash: String) = UserAwareAction { implicit request =>
    cacheApi.getItem[FlightSearchResponse](s"_flight${request.queryString("fl_tabHash").headOption.orNull}") match {
      case None => Redirect(controllers.web.routes.FlightCtrl.closeSession(fl_tabHash))
      case a @ Some(_) =>
        val itinerary = a.get.getPricedItineraryWSResponses.find(_.getCacheIndex == ref.toInt).get
        var isCached = false
        val travellerInfo = cacheApi.getItem[FlightTravellerInfo](s"traveller_$fl_tabHash") match {
          case traveller @ Some(_) =>
            isCached = true; travellerForm.fill(traveller.get)
          case _ => if (request.identity.isDefined) {
            val user = request.identity.get
            val title = user.getPrefix
            travellerForm.fill(FlightTravellerInfo(
              travellersBio = List(TravellerBio(
                title = if (title != null) title.name() else "",
                firstName = user.getFirstName,
                otherName = None,
                lastName = user.getLastName,
                dateOfBirth = "",
                passengerType = "ADT"
              )),
              cacheIndex = itinerary.getCacheIndex.toString,
              contactTitle = if (title != null) title.name else "",
              contactFirstName = user.getFirstName,
              contactLastName = user.getLastName,
              email = user.getEmail,
              phone = user.getPhone,
              salesCategory = itinerary.getSalesCategory.name(),
              remarks = None,
              userId = Some(user.getId.toString),
              privateUserId = None
            ))
          } else travellerForm
        }
        Ok(flights.detail(itinerary, a.get.getFlightSearchRequest, travellerInfo, ref, encrypt.encrypt(request.request.uri), fl_tabHash, isCached))
    }
  }

  def submitTraveller(itineraryRef: String, fl_tabHash: String, uri: String) = UserAwareAction { implicit request =>
    cacheApi.getItem[FlightSearchResponse](s"_flight${request.queryString("fl_tabHash").headOption.orNull}") match {
      case None => Redirect(routes.FlightCtrl.closeSession(fl_tabHash))
      case a @ Some(_) =>
        travellerForm.bindFromRequest().fold(
          error => BadRequest(flights.detail(a.get.getPricedItineraryWSResponses.find(_.getCacheIndex == itineraryRef.toInt).get, a.get.getFlightSearchRequest, error, itineraryRef, uri, fl_tabHash)),
          success => {
            cacheApi.setItem(s"traveller_$fl_tabHash", success)
            Redirect(routes.FlightCtrl.reviewProduct(itineraryRef, fl_tabHash, uri))
          }
        )
    }
  }

  def reviewProduct(itineraryRef: String, fl_tabHash: String, uri: String) = UserAwareAction { implicit request =>
    cacheApi.getItem[FlightSearchResponse](CachedVariables.flightResultKey) match {
      case None => Redirect(routes.FlightCtrl.closeSession(fl_tabHash))
      case a @ Some(_) =>
        val itinerary = a.get.getPricedItineraryWSResponses.find(_.getCacheIndex == itineraryRef.toInt).get
        itinerary.getSearchRequest
        val products = ProductQuery.getProducts(itinerary.getSearchRequest)
        Ok(flights.product(products, productsPaymentForm, itinerary, a.get.getFlightSearchRequest, uri, itineraryRef, fl_tabHash))
    }
  }

  def saveProduct(itineraryRef: String, fl_tabHash: String, uri: String) = UserAwareAction { implicit request =>
    cacheApi.getItem[FlightSearchResponse](s"_flight${request.queryString("fl_tabHash").headOption.orNull}") match {
      case None => Redirect(routes.FlightCtrl.closeSession(fl_tabHash))
      case a @ Some(_) =>
        val itineraries = a.get.getPricedItineraryWSResponses.find(_.getCacheIndex == itineraryRef.toInt)
        val travellerInfoForm = cacheApi.getItem(s"traveller_$fl_tabHash").asInstanceOf[Option[FlightTravellerInfo]].get
        val bookingRequest = new BookingRequest
        bookingRequest.setItineraryWSResponse(itineraries.get)
        bookingRequest.setSupplier(a.get.getFlightSearchRequest.getSupplier)
        bookingRequest.getPassengers.addAll(FlightBookingForm.getFlightPassenger(travellerInfoForm))
        if (request.body.asFormUrlEncoded.get.exists(a => a._1.equals("product_id[]"))) {
          bookingRequest.getProducts.addAll(request.body.asFormUrlEncoded.get("product_id[]").map { p =>
            val addOnProduct = AddonProducts.find.byId(java.lang.Long.parseLong(p))
            val bookingProduct = new BookingProducts
            bookingProduct.setAddonProductOptions(addOnProduct)
            val searchRequest = a.get.getFlightSearchRequest
            val productAmt = BigDecimal((searchRequest.getAdultCount * addOnProduct.getAdultPrice) + (searchRequest.getChildCount * addOnProduct.getChildPrice) + (searchRequest.getInfantCount * addOnProduct.getInfantPrice))
            bookingProduct.setAmount(productAmt.doubleValue())
            bookingProduct
          })
        }
        bookingRequest.setSessionId(a.get.getFlightSearchRequest.getSessionId)
        cacheApi.setItem(s"bookingRequest_$fl_tabHash", bookingRequest)
        Redirect(routes.FlightCtrl.reviewItinerary(itineraryRef, fl_tabHash, uri))
    }
  }

  def reviewItinerary(itineraryRef: String, fl_tabHash: String, uri: String) = UserAwareAction { implicit request =>
    val bookingRequest = cacheApi.getItem[BookingRequest](s"bookingRequest_$fl_tabHash")
    Ok(flights.review(bookingRequest.get, bookingRequest.get.getItineraryWSResponse.getSearchRequest, uri, itineraryRef, fl_tabHash))
  }

  def paymentOption(itineraryRef: String, fl_tabHash: String, uri: String) = UserAwareAction { implicit request =>
    cacheApi.getItem[BookingRequest](s"bookingRequest_$fl_tabHash") match {
      case None => Redirect(request.headers.get(HttpHeaders.FROM).getOrElse("/flight/search")).withNewSession.flashing(("error", "Sorry, Session Expired. Please try Again"))
      case a @ Some(_) =>
        //        val bookingRequest = cacheApi.getItem[BookingRequest](s"bookingRequest_$fl_tabHash")
        //        val itinerary = a.get.getPricedItineraryWSResponses.find(_.getCacheIndex == itineraryRef.toInt).get
        Ok(flights.payment(a.get, a.get.getItineraryWSResponse.getSearchRequest, uri, itineraryRef, fl_tabHash))
    }
  }

  def savePaymentOption(itineraryRef: String, fl_tabHash: String, uri: String) = UserAwareAction.async {
    implicit request =>
      cacheApi.getItem[FlightSearchResponse](s"_flight${
        request.queryString("fl_tabHash").headOption.orNull
      }") match {
        case None => Future.successful(Redirect(routes.FlightCtrl.closeSession(fl_tabHash)))
        case a @ Some(_) =>
          val itineraries = a.get.getPricedItineraryWSResponses.find(_.getCacheIndex == itineraryRef.toInt)
          productsPaymentForm.bindFromRequest().fold(
            error => Future {
              val products = ProductQuery.getProducts(a.get.getFlightSearchRequest); //Product.getInstance().parseItinerary(itineraries.get).get().asScala.toList
              BadRequest(flights.product(products, error, itineraries.get, a.get.getFlightSearchRequest, uri, itineraryRef, fl_tabHash))
            },
            productPurchaseInfo => {
              val travellerInfoForm = cacheApi.getItem(s"traveller_$fl_tabHash").asInstanceOf[Option[FlightTravellerInfo]].get
              val bookingRequest = new BookingRequest
              bookingRequest.setItineraryWSResponse(itineraries.get)
              bookingRequest.setSupplier(a.get.getFlightSearchRequest.getSupplier)
              bookingRequest.getPassengers.addAll(FlightBookingForm.getFlightPassenger(travellerInfoForm))
              //              if (request.body.asFormUrlEncoded.get.exists(a => a._1.equals("product_id[]"))) {
              //                bookingRequest.getProducts.addAll(request.body.asFormUrlEncoded.get("product_id[]").map {
              //                  p =>
              //                    val addOnProduct = AddonProducts.find.byId(java.lang.Long.parseLong(p))
              //                    val bookingProduct = new BookingProducts
              //                    bookingProduct.setAddonProductOptions(addOnProduct)
              //                    //                    bookingProduct.setAmount(addOnProduct.getAmount)
              //                    bookingProduct
              //                })
              //              }
              bookingRequest.setSessionId(a.get.getFlightSearchRequest.getSessionId)
              cacheApi.setItem(s"bookingRequest_$fl_tabHash", bookingRequest)
              Future.successful(Ok(flights.review(bookingRequest, a.get.getFlightSearchRequest, uri, itineraryRef, fl_tabHash)))
            }
          )
      }
  }

  def bookItinerary(itineraryRef: String, fl_tabHash: String) = UserAwareAction.async {
    implicit request =>
      cacheApi.getItem[FlightSearchResponse](s"_flight${request.queryString("fl_tabHash").headOption.orNull}") match {
        case None => Future.successful(Redirect(routes.FlightCtrl.closeSession(fl_tabHash)))
        case a @ Some(_) =>
          val itineraries = a.get.getPricedItineraryWSResponses.find(_.getCacheIndex == itineraryRef.toInt)
          val bookingRequest = cacheApi.getItem[BookingRequest](s"bookingRequest_$fl_tabHash").get
          bookingRequest.setPaymentMethod(request.getQueryString("paymentMethodId").getOrElse("5"))
          bookingRequest.setIncludeSMS(false)
          for {
            pnr <- serviceAPI.flightApi.createPNR(bookingRequest)
            booking <- flightCrud.create(pnr, bookingRequest, request.identity.asInstanceOf[Option[Users]], None)
            //Send Mails to customers
            _ <- Future.successful {
              if (pnr.getStatus.equals(BookingStatus.CONFIRMED)) {
                utils.Mailer.flightBooking(pnr, booking, itineraries.get)
              } else utils.Mailer.failedBooking(booking, itineraries.get)
            }
            result <- Future.successful {

              cacheApi.remove(s"bookingRequest_$fl_tabHash")
              cacheApi.remove(s"traveller_$fl_tabHash")
              cacheApi.remove(CachedVariables.flightResultKey)
              cacheApi.remove(s"${CachedVariables.flightResultKey}_sessionId")
              Redirect(routes.FlightCtrl.confirm(pnr.getStatus.name.toLowerCase(), encrypt.encrypt(booking.getId.toString)))

            }
          } yield result
      }
  }

  def confirm(status: String, id: String) = UserAwareAction {
    implicit request =>
      Option(Bookings.find.byId(java.lang.Long.parseLong(encrypt.decrypt(id)))) match {
        case booking: Some[Bookings] =>
          if (booking.get.getStatus.eq(BookingStatus.CONFIRMED)) {
            Ok(flights.bookingConfirmation(booking.get.getFlightBookings))
          } else {
            Ok(flights.bookingFailed(booking.get.getFlightBookings))
          }
        case _ => Ok("Invalid Booking Reference")
      }

  }

  def closeSession(fl_tabHash: String) = UserAwareAction {
    implicit request =>
      Redirect(routes.FlightCtrl.noResult("se")).flashing(("error", "Your session has expired. Please try again"))
  }
}
