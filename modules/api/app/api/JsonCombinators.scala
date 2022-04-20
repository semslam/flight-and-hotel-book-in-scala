package api

import models.api._
import java.util.Date

import controllers.api.{ Passenger, SearchFlightRequest, Segment }
import flight.dto.entity.{ Passenger => DtoPassenger, _ }
import models.api._
import models.{ Airlines => _, Airports => _, User => _, _ }
import play.api.libs.json._
import play.api.libs.json.Reads.{ DefaultDateReads => _, _ }
import play.api.libs.functional.syntax._
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

/*
* Set of every Writes[A] and Reads[A] for render and parse JSON objects
*/
object JsonCombinators {

  implicit val dateWrites = Writes.dateWrites("dd-MM-yyyy HH:mm:ss")
  implicit val dateReads = Reads.dateReads("dd-MM-yyyy HH:mm:ss")

  implicit val userWrites = new Writes[User] {
    def writes(u: User) = Json.obj(
      "id" -> u.id,
      "email" -> u.email,
      "name" -> u.name
    )
  }
  implicit val usersWrites = new Writes[Users] {
    def writes(u: Users) = Json.obj(
      "id" -> u.getId.toString,
      "email" -> u.getEmail,
      "name" -> u.fullName
    )
  }

  implicit val userReads: Reads[User] =
    (__ \ "name").read[String](minLength[String](1)).map(name => User(0L, null, null, name, false, false))

  //  implicit va ext, deadline) => Task(0L, 0L, 0, text, null, deadline, false))

  //  Flight Result Format
  implicit val airportFormat: Format[Airports] = new Format[Airports] {
    override def writes(o: Airports): JsValue = JsObject(Seq(
      "state" -> JsString(if (o != null) o.getState else ""),
      "code" -> JsString(if (o != null) o.getAirCode else ""),
      "airportName" -> JsString(if (o != null) o.getAirportName else ""),
      "city" -> JsString(if (o != null) o.getCity else ""),
      "countryName" -> JsString(if (o != null) o.getCountryName else ""),
      "countryCode" -> JsString(if (o != null) o.getCountryCode else "")
    ))

    override def reads(json: JsValue): JsResult[Airports] = ???
  }
  implicit val passengerCode: Format[PassengerCode] = new Format[PassengerCode] {
    def reads(json: JsValue) = JsSuccess(PassengerCode.valueOf(json.as[String])) // doesn't compile
    def writes(myEnum: PassengerCode) = JsString(myEnum.toString)
  }
  implicit val tripType: Format[TripType] = new Format[TripType] {
    def reads(json: JsValue) = JsSuccess(TripType.valueOf(json.as[String])) // doesn't compile
    def writes(myEnum: TripType) = JsString(myEnum.toString)
  }
  implicit val cabinClass: Format[CabinClass] = new Format[CabinClass] {
    def reads(json: JsValue) = JsSuccess(CabinClass.valueOf(json.as[String])) // doesn't compile
    def writes(myEnum: CabinClass) = JsString(myEnum.toString)
  }
  implicit val cabinPrefLevel: Format[CabinPrefLevel] = new Format[CabinPrefLevel] {
    def reads(json: JsValue) = JsSuccess(CabinPrefLevel.valueOf(json.as[String])) // doesn't compile
    def writes(myEnum: CabinPrefLevel) = JsString(myEnum.toString)
  }
  implicit val ticketType: Format[TicketType] = new Format[TicketType] {
    def reads(json: JsValue) = JsSuccess(TicketType.valueOf(json.as[String])) // doesn't compile
    def writes(myEnum: TicketType) = JsString(myEnum.toString)
  }
  implicit val flightType: Format[FlightType] = new Format[FlightType] {
    def reads(json: JsValue) = JsSuccess(FlightType.valueOf(json.as[String])) // doesn't compile
    def writes(myEnum: FlightType) = JsString(myEnum.toString)
  }
  implicit val ticketPolicy: Format[TicketPolicy] = new Format[TicketPolicy] {
    def reads(json: JsValue) = JsSuccess(TicketPolicy.valueOf(json.as[String])) // doesn't compile
    def writes(myEnum: TicketPolicy) = JsString(myEnum.toString)
  }
  implicit val salesCategory: Format[SalesCategory] = new Format[SalesCategory] {
    def reads(json: JsValue) = JsSuccess(SalesCategory.valueOf(json.as[String])) // doesn't compile
    def writes(myEnum: SalesCategory) = JsString(myEnum.toString)
  }
  implicit val ticketLocale: Format[TicketLocale] = new Format[TicketLocale] {
    def reads(json: JsValue) = JsSuccess(TicketLocale.valueOf(json.as[String])) // doesn't compile
    def writes(myEnum: TicketLocale) = JsString(myEnum.toString)
  }
  implicit val airlinesFormat: Format[Airlines] = new Format[Airlines] {
    override def reads(json: JsValue): JsResult[Airlines] = ???

    override def writes(o: Airlines): JsValue = JsObject(Seq(
      "name" -> JsString(o.getName),
      "airlineCode" -> JsString(o.getAirlineCode)
    ))
  }
  implicit val ticketRuleFormat: Format[TicketRule] = new Format[TicketRule] {
    override def reads(json: JsValue): JsResult[TicketRule] = ???

    override def writes(o: TicketRule): JsValue = JsObject(Seq(
      "arrivalAirport" -> JsString(o.getArrivalAirport),
      "departureAirport" -> JsString(o.getDepartureAirport),
      "departureDate" -> JsString(o.getDepartureDate),
      "filingAirline" -> JsString(o.getFilingAirline),
      "message" -> JsString(o.getMessage),
      "referenceCode" -> JsString(o.getReferenceCode)
    ))
  }
  implicit val ticketInfoFormat: Format[TicketingInfoWSResponse] = new Format[TicketingInfoWSResponse] {
    override def reads(json: JsValue): JsResult[TicketingInfoWSResponse] = ???

    override def writes(o: TicketingInfoWSResponse): JsValue = JsObject(Seq(
      "eTicketEligibility" -> JsString(o.geteTicketEligibility()),
      "eTicketEligibility" -> JsString(o.getTicketTimeLimit.toString)
    //      "ticketType" -> Json.toJson(o.getTicketType)
    ))
  }
  implicit val segmentFormat: Format[Segment] = Json.format[Segment]
  implicit val passengerTypeFormat: Format[PassengerType] = new Format[PassengerType] {
    override def reads(json: JsValue): JsResult[PassengerType] = ???

    override def writes(o: PassengerType): JsValue = JsObject(Seq(
      "quantity" -> JsNumber(o.getQuantity.toInt),
      "code" -> JsString(o.getCode.value())
    ))
  }
  implicit val passengerDtoFormat: Format[DtoPassenger] = new Format[DtoPassenger] {
    override def reads(json: JsValue): JsResult[DtoPassenger] = ???

    override def writes(o: DtoPassenger): JsValue = JsObject(Seq())
  }
  implicit val passengerReadFormat: Reads[controllers.api.Passenger] = (
    (JsPath \ "quantity").read[Int] and
    (JsPath \ "code").read[String]
  )(Passenger.apply _)
  implicit val passengerFormat = new Format[Passenger] {
    override def reads(json: JsValue): JsResult[Passenger] = passengerReadFormat.reads(json)

    override def writes(o: Passenger): JsValue = JsObject(Seq(
      "quantity" -> JsNumber(o.quantity),
      "code" -> JsString(o.code)
    ))
  }
  implicit val flightItineraryFormat: Format[SearchFlightRequest] = Json.format[SearchFlightRequest]
  implicit val flightSearchRequestWriteFormat: Writes[FlightSearchRequest] = new Writes[FlightSearchRequest] {
    override def writes(o: FlightSearchRequest): JsValue = JsObject(Seq(
      "sessionId" -> JsString(o.getSessionId),
      "supplier" -> JsString(o.getSupplier),
      "tabHash" -> JsString(o.getTabHash),
      "adultCount" -> JsNumber(o.getAdultCount),
      "childCount" -> JsNumber(o.getChildCount),
      "infantCount" -> JsNumber(o.getInfantCount),
      "originDestinationRequests" -> JsArray(o.getOriginDestinationRequests.map { originDestinations =>
        JsObject(Seq(
          "RPH" -> JsString(originDestinations.getRPH),
          "origin" -> JsString(originDestinations.getOrigin),
          "destination" -> JsString(originDestinations.getDestination),
          "departureDateTime" -> JsString(originDestinations.getDepartureDateTime)
        ))
      }),
      "passengerType" -> JsArray(o.getPassengerTypes.map { passenger =>
        JsObject(Seq(
          "code" -> Json.toJson(passenger.getCode),
          "quantity" -> JsNumber(BigDecimal(passenger.getQuantity))
        ))
      }),
      "seatsCount" -> JsNumber(o.getSeatCount),
      "airlines" -> JsArray(o.getPreferredAirline.map(JsString)),
      "cabin" -> Json.toJson(o.getPreferredCabin),
      "saleCategory" -> Json.toJson(o.getSalesCategory),
      "ticketLocale" -> Json.toJson(o.getTicketLocale),
      "tripType" -> Json.toJson(o.getTripType)
    ))
  }
  implicit val taxFareFormat: Writes[TaxFare] = new Writes[TaxFare] {
    override def writes(o: TaxFare): JsValue = JsObject(Seq(
      "currency" -> JsString(o.getCurrency),
      "taxCode" -> JsString(o.getTaxCode),
      "description" -> JsString(o.getTaxDescription),
      "amount" -> JsNumber(o.getAmount.doubleValue())
    ))
  }
  implicit val baseFareFormat: Format[BaseFare] = new Format[BaseFare] {
    override def reads(json: JsValue): JsResult[BaseFare] = ???

    override def writes(o: BaseFare): JsValue = JsObject(Seq(
      "currencyCode" -> JsString(o.getCurrencyCode),
      "amount" -> JsNumber(o.getAmount)
    ))
  }
  implicit val passengerFareFormat: Format[PassengerFare] = new Format[PassengerFare] {
    override def reads(json: JsValue): JsResult[PassengerFare] = ???

    override def writes(o: PassengerFare): JsValue = JsObject(Seq())
  }
  implicit val ticketingRuleFormat: Format[TicketingRule] = new Format[TicketingRule] {
    override def reads(json: JsValue): JsResult[TicketingRule] = ???

    override def writes(o: TicketingRule): JsValue = JsObject(Seq())
  }
  implicit val fareBreakDownFormat: Format[FareBreakDown] = new Format[FareBreakDown] {
    override def reads(json: JsValue): JsResult[FareBreakDown] = ???

    override def writes(o: FareBreakDown): JsValue = JsObject(Seq())
  }
  implicit val flightSegmentWrite: Writes[FlightSegmentWSResponse] = new Writes[FlightSegmentWSResponse] {
    override def writes(o: FlightSegmentWSResponse): JsValue = JsObject(Seq(
      "departureAirport" -> JsString(o.getDepartureAirport),
      "arrivalAirport" -> JsString(o.getArrivalAirport),
      "departureAirportCode" -> JsString(o.getDepartureAirportCode),
      "departureTerminal" -> JsString(o.getDepartureTerminal),
      "arrivalAirportCode" -> JsString(o.getArrivalAirportCode),
      "arrivalTerminal" -> JsString(o.getArrivalTerminal),
      "airportCodeContext" -> JsString(o.getAirportCodeContext),
      "departureTimeZone" -> JsString(o.getDepartureTimeZone),
      "arrivalTimeZone" -> JsString(o.getArrivalTimeZone),
      "departureDateTime" -> JsString(o.getDepartureDateTime),
      "arrivalDateTime" -> JsString(o.getArrivalDateTime),
      "duration" -> JsString(o.getDuration),
      "marketingAirline" -> Json.toJson(o.getMarketingAirline),
      "marketingAirlineCode" -> JsString(o.getMarketingAirlineCode),
      "operatingAirline" -> Json.toJson(o.getOperatingAirline),
      "operatingAirlineCode" -> JsString(o.getOperatingAirlineCode),
      "cabin" -> JsString(o.getCabin),
      "RPH" -> JsString(o.getRPH),
      "flightNumber" -> JsString(o.getFlightNumber),
      "resBookDesigCode" -> JsString(o.getResBookDesigCode),
      "airEquipType" -> JsString(o.getAirEquipType),
      "numberInParty" -> JsNumber(BigDecimal(o.getNumberInParty)),
      "eTicketEligible" -> JsBoolean(o.isETicketEligible()),
      "mealCode" -> JsString(o.getMealCode()),
      "stopQuantity" -> JsNumber(BigDecimal(o.getStopQuantity)),
      "elapseTime" -> JsString(o.getElapseTime),
      "originDestinationType" -> JsString(o.getOriginDestinationType),
      "marriageGrp" -> JsString(o.getMarriageGrp)
    ))
  }
  implicit val priceInfoFormat: Format[PricingInfoWSResponse] = new Format[PricingInfoWSResponse] {
    override def writes(o: PricingInfoWSResponse): JsValue = JsObject(Seq(
      "gdsBaseFare" -> JsNumber(BigDecimal(o.getGdsBaseFare)),
      "gdsTaxFare" -> JsNumber(BigDecimal(o.getGdsTaxFare)),
      "gdsTotalFare" -> JsNumber(BigDecimal(o.getGdsTotalFare)),
      "gdsEquivCurrency" -> JsString(o.getGdsEquivCurrency),
      //      "gdsEquivBaseFare" -> JsNumber(BigDecimal(o.getGdsEquivBaseFare)),
      "baseFare" -> Json.toJson(o.getBaseFare),
      "totalTax" -> Json.toJson(o.getTotalTax.toList),
      "internalTaxes" -> Json.toJson(o.getInternalTaxes.toList),
      "totalFare" -> JsNumber(BigDecimal(o.getTotalFare)),
      "currencyCode" -> JsString(o.getCurrencyCode),
      "decimalPlaces" -> JsNumber(BigDecimal(o.getDecimalPlaces)),
      "pricingSource" -> JsString(o.getPricingSource),
      //      "adultBaseFair" -> JsNumber(BigDecimal(o.getAdultBaseFair)),
      //      "childrenBaseFair" -> JsNumber(BigDecimal(o.getChildrenBaseFair)),
      //      "infantBaseFair" -> JsNumber(BigDecimal(o.getInfantBaseFair)),
      //      "adultTaxFair" -> JsNumber(BigDecimal(o.getAdultTaxFair)),
      //      "childrenTaxFair" -> JsNumber(BigDecimal(o.getChildrenTaxFair)),
      //      "infantTaxFair" -> JsNumber(BigDecimal(o.getInfantTaxFair)),
      //      "adultTotal" -> JsNumber(BigDecimal(o.getAdultTotal)),
      //      "childrenTotal" -> JsNumber(BigDecimal(o.getChildrenTotal)),
      //      "infantTotal" -> JsNumber(BigDecimal(o.getInfantTotal)),
      //      "totalMarkupFare" -> JsNumber(o.getTotalMarkupFare()),
      //      "adtMarkUp" -> JsNumber(o.getAdtMarkUp()),
      //      "chdMarkUp" -> JsNumber(o.getChdMarkUp()),
      //      "infMarkUp" -> JsNumber(o.getInfMarkUp()),
      //      "fareBreakDown" -> Json.toJson(o.getFareBreakDown()),
      "priceDirection" -> JsString(o.getPriceDirection),
      "dispenseValue" -> JsNumber(o.getDispenseValue()),
      "dispenseValueAmount" -> JsNumber(o.getDispenseValueAmount())
    ))

    override def reads(json: JsValue): JsResult[PricingInfoWSResponse] = ???
  }
  implicit val originDestinations: Format[OriginDestinationRequest] = new Format[OriginDestinationRequest] {
    override def writes(o: OriginDestinationRequest): JsValue = JsObject(Seq())

    override def reads(json: JsValue): JsResult[OriginDestinationRequest] = ???
  }
  implicit val originDestinationWrite: Format[OriginDestinationWSResponse] = new Format[OriginDestinationWSResponse] {
    override def reads(json: JsValue): JsResult[OriginDestinationWSResponse] = ???

    override def writes(o: OriginDestinationWSResponse): JsValue = JsObject(Seq(
      "flightSegmentWSResponses" -> Json.toJson(o.getFlightSegmentWSResponses.toList),
      "originAirport" -> JsString(o.getOriginAirport),
      "destinationAirport" -> JsString(o.getDestinationAirport),
      "originAirportCode" -> JsString(o.getOriginAirportCode),
      "destinationAirportCode" -> JsString(o.getDestinationAirport),
      "arrivalDateTime" -> JsString(o.getArrivalDateTime),
      "departureDateTime" -> JsString(o.getDepartureDateTime),
      "duration" -> JsString(o.getDuration),
      //      "isNextDayArrival" -> JsBoolean(o.getIsNextDayArrival()),
      "marketingAirline" -> Json.toJson(o.getMarketingAirline),
      "marketingAirlineCode" -> JsString(o.getMarketingAirlineCode),
      "operatingAirline" -> Json.toJson(o.getOperatingAirline),
      "operatingAirlineCode" -> JsString(o.getOperatingAirlineCode),
      "cabin" -> JsString(o.getCabin),
      "isMultiAirline" -> JsBoolean(o.isMultiAirline),
      "numberOfStops" -> JsNumber(o.getNumberOfStops()),
      "terminal" -> JsString(o.getTerminal),
      //      "directionId" -> JsNumber(BigDecimal(o.getDirectionId())),
      //      "refNumber" -> JsNumber(BigDecimal(o.getRefNumber)),
      "tripCode" -> JsString(o.getTripCode),
      "originAirportTbl" -> Json.toJson(o.getOriginAirportTbl),
      "destinationAirportTbl" -> Json.toJson(o.getDestinationAirportTbl),
      "layover" -> JsString(o.getLayOver)
    ))
  }
  implicit val originDestinationCombinations: Format[OriginDestinationCombinations] = new Format[OriginDestinationCombinations] {
    override def writes(o: OriginDestinationCombinations): JsValue = ???

    override def reads(json: JsValue): JsResult[OriginDestinationCombinations] = ???
  }
  implicit val airItineraryFormat: Writes[AirItineraryWSResponse] = new Writes[AirItineraryWSResponse] {
    override def writes(o: AirItineraryWSResponse): JsValue = JsObject(Seq(
      "originDestinationWSResponses" -> Json.toJson(o.getOriginDestinationWSResponses.toList),
      "directionIndicator" -> JsString(o.getDirectionIndicator)
    ))
  }
  implicit val itineraryWSResponseFormat: Writes[ItineraryWSResponse] = new Writes[ItineraryWSResponse] {
    override def writes(o: ItineraryWSResponse): JsValue = JsObject(Seq(
      "airItineraryWSResponse" -> Json.toJson(o.getAirItineraryWSResponse),
      "pricingInfoWSResponse" -> Json.toJson(o.getPricingInfoWSResponse),
      "ticketingInfoWSResponse" -> Json.toJson(o.getTicketingInfoWSResponse),
      "airline" -> Json.toJson(o.getAirline),
      "airlineCode" -> JsString(o.getAirlineCode),
      "direction" -> JsNumber(BigDecimal(o.getDirection)),
      "cabin" -> JsString(o.getCabin),
      "cache" -> JsNumber(o.getCacheIndex.toInt),
      "supplier" -> JsString(o.getSupplier),
      "ticketPolicy" -> Json.toJson(o.getTicketPolicy),
      "salesCategory" -> Json.toJson(o.getSalesCategory),
      "ticketLocale" -> Json.toJson(o.getTicketLocale),
      "sessionId" -> JsString(o.getSessionId),
      "sequenceNumber" -> JsString(o.getSequenceNumber),
      "selectedCombinationId" -> JsString(o.getSelectedCombinationId)
    ))
  }
  implicit val searchResponseWriteFormat: Format[FlightSearchResponse] = new Format[FlightSearchResponse] {
    override def writes(o: FlightSearchResponse): JsValue = JsObject(Seq(
      "flightSearchRequest" -> Json.toJson(o.getFlightSearchRequest),
      "pricedItineraryWSResponses" -> Json.toJson(o.getPricedItineraryWSResponses.toList)
    ))

    override def reads(json: JsValue): JsResult[FlightSearchResponse] = ???
  }

}