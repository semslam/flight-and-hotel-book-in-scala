package models

import flight.dto.entity.FlightSearchRequest
import scala.collection.JavaConversions._

object ProductQuery extends App {

  def getProducts(searchRequest: FlightSearchRequest): List[ProductResult] = {
    val products = AddonProducts.find.all()
    //      .filterNot(record =>
    //        record.getDepartureCountryOptions.contains(searchRequest.getOriginDestinationRequests.head.getOrigin) && record.getArrivingCountryOptions.contains(searchRequest.getOriginDestinationRequests.last.getDestination))
    products.map(product => ProductResult(product, BigDecimal((searchRequest.getAdultCount * product.getAdultPrice) + (searchRequest.getChildCount * product.getChildPrice) + (searchRequest.getInfantCount * product.getInfantPrice)))).toList
  }

}

case class ProductResult(addonProducts: AddonProducts, totalAmount: BigDecimal)
