package controllers.common

import java.text.SimpleDateFormat
import java.util.Date

import api.ServicesAPI
import caching.CacheApi
import com.alajobi.ota.utils.CachedVariables
import crypto.Encrypt
import flight.dto.entity._
import mailer.MailService
import models._
import play.api._
import play.api.mvc._
import com.alajobi.ota.utils.SupplierManager.Implicits.flightSupplier
import javax.inject.{ Inject, Singleton }
import play.api.libs.json.{ JsNumber, JsObject, JsString }

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class Application @Inject() (val services: ServicesAPI, implicit val encrypt: Encrypt, implicit val mailService: MailService) extends Controller {

  implicit val cacheApi = services.cache

  def status = Action {
    Ok("Everything is great!")
  }

  def link(id: Long) = Action.async { implicit request =>
    import scala.collection.JavaConversions._
    val cmsLinks = CmsLinks.find.byId(id)
    if (cmsLinks != null) {
      cmsLinks.getServiceType match {
        case "FLIGHT" =>
          val flightSearchRequest = new FlightSearchRequest
          var tripType = TripType.RETURN
          val tripSize = cmsLinks.getLinkSegments.size()
          if (cmsLinks.getLinkSegments.size() == 1) {
            tripType = TripType.ONE_WAY
          } else if (tripSize > 2) {
            tripType = TripType.MULTI_CITY
          }
          flightSearchRequest.setTripType(tripType)
          flightSearchRequest.setAdultCount(cmsLinks.getAdtUnit)
          flightSearchRequest.setChildCount(cmsLinks.getChdUnit)
          flightSearchRequest.setInfantCount(cmsLinks.getInfUnit)
          flightSearchRequest.setSalesCategory(SalesCategory.B2C)
          flightSearchRequest.setTabHash("tablswdisa93weajxwqn")
          flightSearchRequest.setTicketLocale(TicketLocale.International)
          flightSearchRequest.setSupplier(flightSupplier.getName)
          flightSearchRequest.setPreferredCabin(CabinClass.ECONOMY)
          val passengerType: PassengerType = new PassengerType
          passengerType.setCode(PassengerCode.ADULT)
          passengerType.setQuantity(flightSearchRequest.getAdultCount)
          flightSearchRequest.getPassengerTypes.add(passengerType)
          if (flightSearchRequest.getChildCount > 0) {
            val child: PassengerType = new PassengerType
            child.setCode(PassengerCode.CHILD)
            child.setQuantity(flightSearchRequest.getChildCount)
            flightSearchRequest.getPassengerTypes.add(child)
          }
          if (flightSearchRequest.getInfantCount > 0) {
            val infant: PassengerType = new PassengerType
            infant.setCode(PassengerCode.INFANT)
            infant.setQuantity(flightSearchRequest.getInfantCount)
            flightSearchRequest.getPassengerTypes.add(infant)
          }
          val dtFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
          flightSearchRequest.getOriginDestinationRequests.addAll(cmsLinks.getLinkSegments.map { item =>
            val originDestinationRequest = new OriginDestinationRequest()
            originDestinationRequest.setOrigin(item.getDepartAirCode)
            originDestinationRequest.setDestination(item.getArrivalAirCode)
            //Check if Current date is expired
            if (item.getDate.before(new Date) && item.getRelativeDate) originDestinationRequest.setDepartureDateTime(dtFormat.format(new Date)) else originDestinationRequest.setDepartureDateTime(dtFormat.format(item.getDate))
            originDestinationRequest
          })
          for {
            searchResponse <- services.flightApi.searchFlight(flightSearchRequest)
            flightFareResponse <- services.flightFareRuleManager(searchResponse, reduceWithHoldingTax = false, 0.0)
            _ <- Future {
              cacheApi.setItem(s"_flight${flightSearchRequest.getTabHash}", flightFareResponse, 14 minutes)
              cacheApi.setItem(s"${flightSearchRequest.getTabHash}_sessionId", searchResponse.getFlightSearchRequest.getSessionId)
              //              sessionHandler.assign(flightSearchRequest.getTabHash, flightSearchRequest.getSessionId)
            }
          } yield Redirect(s"/flight/result/${flightSearchRequest.getTicketLocale.name().toLowerCase}?fl_tabHash=${flightSearchRequest.getTabHash}")
        case "CONTENT_CATEGORY" => Future.successful(Ok(""))
        case "URL" => Future.successful(Redirect(cmsLinks.getHref))
      }
    } else Future.successful(Ok(""))
  }

}