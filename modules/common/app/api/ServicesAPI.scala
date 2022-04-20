package api

import java.util.Calendar
import javax.inject.{ Inject, Named, Singleton }

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import akka.actor.Actor.Receive
import com.alajobi.ota.utils.SupplierManager
import caching.CacheApi
import com.alajobi.ota.flights.FlightFareRuleService
import flight.dto.entity.{ BookingRequest, FlightSearchRequest }
import flight.http.impl.FlightServicesAPI
import hotel.http.impl.api.HotelServicesAPI
import models.Airports
import product.hepstar.dto.entity.requests.{ PolicyPriceRequest, PolicyRequest }
import product.impl.ProductsServicesAPI

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

/**
 * Created by Igbalajobi Jamiu O. on 18/09/2016 3:09 PM
 */
@Singleton
class ServicesAPI @Inject() (val flightApi: FlightServicesAPI, val hotelApi: HotelServicesAPI, val productsApi: ProductsServicesAPI, val flightFareRuleManager: FlightFareRuleService, @Named("PlayCache") val cache: CacheApi, val actorSystem: ActorSystem = ActorSystem("appAkka")) /* extends Actor with ActorLogging */ {

  lazy val flightSupplier = SupplierManager.Implicits.flightSupplier

  def queryHepstarPolicies(flightSearchRequest: FlightSearchRequest, bookingRequest: BookingRequest) = {
    val policyRequest = new PolicyRequest
    val originAirport = Airports.getAirport(flightSearchRequest.getOriginDestinationRequests.head.getOrigin)
    policyRequest.setDepartureDate(flightSearchRequest.getOriginDestinationRequests.head.getDepartureDateTime.split("T")(0))
    policyRequest.setArrivalDate(flightSearchRequest.getOriginDestinationRequests.last.getDepartureDateTime.split("T")(0))
    policyRequest.setDepartureCountryCode(originAirport.getCountryId.getIsoCode2)
    var destinations = List[String]()
    flightSearchRequest.getOriginDestinationRequests.foreach { airportCode =>
      if (!policyRequest.getDepartureCountryCode.equalsIgnoreCase(Airports.getAirport(airportCode.getDestination).getCountryId.getIsoCode2))
        destinations ::= Airports.getAirport(airportCode.getDestination).getCountryId.getIsoCode2
    }
    policyRequest.setArrivalCountriesCode(destinations.asJava)
    //Set the booking request
    var index = 1
    val policyPriceRequests = bookingRequest.getPassengers.map { passenger =>
      val policyPriceRequest = new PolicyPriceRequest
      policyPriceRequest.setIndex(index.toString)
      index += 1
      policyPriceRequest.setAge((Calendar.getInstance().getWeekYear - passenger.getBirthDate.getYear).toString)
      policyPriceRequest.setResidency(policyRequest.getDepartureCountryCode)
      policyPriceRequest
    }
    policyRequest.setPolicyPriceRequest(policyPriceRequests.asJava)

    /**
     * Call the Hepstar API to get the policies available
     */
    //    null
    //    productsApi.performHepStarPricedPolicy(policyRequest = policyRequest)
    null
  }
}