package models

import flight.dto.entity.ItineraryWSResponse
import scala.collection.JavaConversions._

/**
 * Created by Igbalajobi Jamiu on 6/24/17.
 */
class FlightResultSorting(itineraries: List[ItineraryWSResponse]) {

  def sortAirline: Map[String, List[ItineraryWSResponse]] = {
    var groupByAirline = Map[String, List[ItineraryWSResponse]]()
    itineraries.foreach { map =>
      groupByAirline += map.getAirlineCode -> itineraries.filter(a => a.getAirlineCode.equalsIgnoreCase(map.getAirlineCode))
    }
    groupByAirline
  }

  def sortMatrixTable: List[(String, String, ItineraryWSResponse)] = {
    var dates = List[(String, String, ItineraryWSResponse)]()
    for (itinerary <- itineraries) {
      val departure = itinerary.getAirItineraryWSResponse.getOriginDestinationWSResponses.head.getDepartureDateTime.split("T").head
      val arrival = itinerary.getAirItineraryWSResponse.getOriginDestinationWSResponses.last.getDepartureDateTime.split("T").head
      if (!dates.exists(_._1.equals(departure)) && !dates.exists(_._2.equals(arrival))) dates ::= (departure, arrival, itinerary)
    }

    //    itineraries.foreach { itinerary =>
    //      val departureDate = itinerary.getAirItineraryWSResponse.getOriginDestinationWSResponses.head.getDepartureDateTime.split("T").head
    //      departureArrival ::= (departureDate, arrivalDate)
    //    }
    //    departureArrival.map { a =>
    //      val (departureDate, arrivalDate) = a
    //      (departureDate, arrivalDate, itineraries.filter(a => a.getAirItineraryWSResponse.getOriginDestinationWSResponses.head.getDepartureDateTime.split("T").head.equals(departureDate)))
    //    }
    dates
  }

}
