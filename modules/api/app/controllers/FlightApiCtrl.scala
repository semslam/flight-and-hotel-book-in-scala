package controllers.api

import api.ApiError._
import api.JsonCombinators._
import akka.actor.ActorSystem
import api.ServicesAPI
import com.alajobi.ota.flights.HttpRequestParser
import com.alajobi.ota.utils.CachedVariables
import crypto.Encrypt
import flight.dto.entity._
import javax.inject.Inject
import mailer.MailService
import models.{ Airports, ApiSuppliers, CabinClass, TripType }
import play.api.i18n.{ Messages, MessagesApi }
import play.api.libs.json._

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import com.alajobi.ota.utils.SupplierManager.Implicits.flightSupplier

class FlightApiCtrl @Inject() (val messagesApi: MessagesApi, system: ActorSystem, val services: ServicesAPI, implicit val encrypt: Encrypt, implicit val mailService: MailService) extends api.ApiController {

  implicit val airportFormat: Format[Airports] = new Format[Airports] {
    override def writes(o: Airports): JsValue = JsObject(Seq(
      "state" -> JsString(o.getState),
      "code" -> JsString(o.getAirCode),
      "airportName" -> JsString(o.getAirportName),
      "city" -> JsString(o.getCity),
      "countryName" -> JsString(o.getCountryName),
      "countryCode" -> JsString(o.getCountryCode)
    ))

    override def reads(json: JsValue): JsResult[Airports] = ???
  }

  def suggestAirport(city: String) = ApiAction { implicit request =>
    val flightResult = Airports.find.where().raw(s" air_code = '$city' or airport_name like '%$city%' or state like '%$city%' or country_name like '%$city%' order by case when `air_code` = '$city' then 1 else 2 end, air_code ").findList().toVector
    created(flightResult)
  }

  def searchFlight() = SecuredApiActionWithBody { implicit request =>
    readFromRequest[SearchFlightRequest] { flightSearchRequest =>
      for {
        searchResponse <- if (flightSearchRequest.isFlexibleDate) services.flightApi.searchFlightMatrix(flightSearchRequest) else services.flightApi.searchFlight(flightSearchRequest)
        flightFareResponse <- services.flightFareRuleManager(searchResponse, reduceWithHoldingTax = false, 0.0)
        _ <- Future(services.cache.setItem(flightSearchRequest.sessionId, flightFareResponse, 30 minutes))
        result <- created(flightFareResponse)
      } yield result
    }
  }

  def savedSearchFlight(sessionId: String) = SecuredApiAction { implicit request =>
    services.cache.getItem[FlightSearchResponse](sessionId) match {
      case None => noContent()
      case a @ Some(_) => created(a.get)
    }
  }

  def availability(sessionId: String, refId: String) = SecuredApiAction { implicit request =>
    services.cache.getItem[FlightSearchResponse](sessionId) match {
      case None => noContent()
      case a @ Some(_) => created(a.get.getPricedItineraryWSResponses.find(_.getSessionId == refId))
    }
  }

  implicit def implicitSearchRequest(httpRequest: SearchFlightRequest): FlightSearchRequest = {
    val flightSearchRequest = new FlightSearchRequest
    flightSearchRequest.setAdultCount(httpRequest.passengers.count(_.code.equals(PassengerCode.ADULT.name())))
    flightSearchRequest.setChildCount(httpRequest.passengers.count(_.code.equals(PassengerCode.CHILD.name())))
    flightSearchRequest.setInfantCount(httpRequest.passengers.count(_.code.equals(PassengerCode.INFANT.name())))
    flightSearchRequest.setFlexibleDate(httpRequest.flexibleDate.getOrElse(false))
    flightSearchRequest.setPreferredCabin(CabinClass.fromValue(httpRequest.preferredCabin.getOrElse("ECONOMY")))
    flightSearchRequest.setCabinPrefLevel(CabinPrefLevel.PREFERRED)
    if (httpRequest.airlines.isDefined) {
      flightSearchRequest.getPreferredAirline.add(httpRequest.airlines.get.head)
    }
    val originDestinationRequests = httpRequest.segments.zipWithIndex.toList.map { p =>
      val (segment, index) = p
      val originDestinationRequest = new OriginDestinationRequest()
      originDestinationRequest.setOrigin(segment.origin)
      originDestinationRequest.setDestination(segment.arriving)
      originDestinationRequest.setDepartureDateTime(segment.departureDate)
      originDestinationRequest.setRPH((index + 1) toString)
      originDestinationRequest
    }
    flightSearchRequest.getOriginDestinationRequests.addAll(originDestinationRequests)
    flightSearchRequest.setTripType(if (httpRequest.segments.size == 1) TripType.ONE_WAY else if (httpRequest.segments.size > 2) TripType.MULTI_CITY else TripType.RETURN)
    flightSearchRequest.getPassengerTypes.addAll(httpRequest.passengers.map(a => new PassengerType(PassengerCode.valueOf(a.code), a.quantity)))
    flightSearchRequest.setSupplier(flightSupplier.getName)
    flightSearchRequest.setTicketPolicy(HttpRequestParser.getTicketPolicy(flightSearchRequest.getOriginDestinationRequests.toList))
    flightSearchRequest.setSalesCategory(models.SalesCategory.B2C)
    val ticketLocale = HttpRequestParser.getLocale(flightSearchRequest.getOriginDestinationRequests.toList)
    flightSearchRequest.setTicketLocale(ticketLocale)
    flightSearchRequest
  }

}

case class Segment(origin: String, arriving: String, departureDate: String)

case class Passenger(quantity: Int, code: String)

case class SearchFlightRequest(
  segments: Seq[Segment],
  passengers: Seq[Passenger],
  supplier: Option[String],
  flexibleDate: Option[Boolean] = None,
  directFlight: Option[Boolean] = None,
  refundable: Option[Boolean] = None,
  saleCategory: Option[String],
  sessionId: String,
  airlines: Option[Seq[String]],
  preferredCabin: Option[String] = Some(CabinClass.ECONOMY.value())
)

