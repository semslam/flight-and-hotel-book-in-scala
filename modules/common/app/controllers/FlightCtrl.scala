package controllers.common

import java.net.URLEncoder
import javax.inject.{ Inject, Singleton }

import api.ServicesAPI
import com._
import com.alajobi.ota.flights._
import com.alajobi.ota.utils.CachedVariables
import dto.enums.Supplier
import flight.dto.entity.{ BookingRequest, FlightSearchRequest, FlightSearchResponse, ItineraryWSResponse }
import models._
import play.api.libs.json.{ JsNumber, JsObject, JsString }
import play.api.mvc.{ Request, Action, Controller, Result }
import mailer.MailService
import com.alajobi.ota.utils.SupplierManager.Implicits.flightSupplier
import com.google.common.net.HttpHeaders
import crypto.{ Encrypt, TransRefAlgorithm }
import models.AppFeatureLibraries._

import scala.concurrent.duration._
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by Igbalajobi Jamiu Okunade on 4/16/17.
 */
@Singleton
class FlightCtrl @Inject() (val services: ServicesAPI, implicit val encrypt: Encrypt, implicit val mailService: MailService) extends Controller {

  implicit val cacheApi = services.cache

  val travellerForm = FlightBookingForm.travellerForm

  val productsPaymentForm = FlightBookingForm.productsPaymentForm

  private def getSearchResponse(implicit request: Request[_]): FlightSearchResponse = cacheApi.getItem[FlightSearchResponse](CachedVariables.flightResultKey).orNull

  def search(dataType: String) = Action.async { implicit request =>
    val searchRequest: FlightSearchRequest = HttpRequestParser.flight(SalesCategory.valueOf(request.getQueryString("sale_category").orNull))
    for {
      searchResponse <- if (searchRequest.isFlexibleDate) services.flightApi.searchFlightMatrix(searchRequest) else services.flightApi.searchFlight(searchRequest)
      flightFareResponse <- services.flightFareRuleManager(searchResponse, reduceWithHoldingTax = false, 0.0)
      _ <- Future(cacheApi.setItem(CachedVariables.flightResultKey, flightFareResponse, 25 minutes))
    } yield if (request.getQueryString("result_page").nonEmpty) {
      Redirect(s"/flight/result/${searchRequest.getTicketLocale.name().toLowerCase}?fl_tabHash=${searchRequest.getTabHash}")
    } else Ok(JsObject(Map(
      "responseCode" -> JsNumber(if (flightFareResponse.getPricedItineraryWSResponses.isEmpty) 500 else 200),
      "region" -> JsString(searchRequest.getTicketLocale.name().toLowerCase())
    )))
  }

  /**
   * Cache the form response data
   *
   * @param itineraryRef
   * @param fl_tabHash
   * @return
   */
  def storePassengerRecords(itineraryRef: String, fl_tabHash: String) = Action { implicit request =>
    cacheApi.getItem[FlightSearchResponse](CachedVariables.flightResultKey) match {
      case searchResponse: Some[FlightSearchResponse] => travellerForm.bindFromRequest().fold(
        errorForm => Redirect(request.uri).withNewSession.flashing(("error", "Request failed. Please try again.")),
        success => {
          val formValue = request.body.asFormUrlEncoded.get
          //          val passengers = FlightTravellerInfo.getFlightPassenger(success, formValue, "")
          val bookingRequest = new BookingRequest
          val selectedItinerary = searchResponse.get.getPricedItineraryWSResponses.find(_.getCacheIndex == itineraryRef.toInt).get
          bookingRequest.setItineraryWSResponse(selectedItinerary)
          //          bookingRequest.getPassengers.addAll(passengers.toStream.toList.asJava)
          cacheApi.setItem(s"bookingRequest_$fl_tabHash", bookingRequest)
          Redirect(s"/flight/booking/product-checkout?itineraryRef=$itineraryRef&fl_tabHash=$fl_tabHash")
        }
      )
      case _ => Redirect(request.headers.get(HttpHeaders.FROM).getOrElse("/flight/search"))
        .withNewSession.flashing(("error", "Request failed. Please try again."))
    }
  }

}
