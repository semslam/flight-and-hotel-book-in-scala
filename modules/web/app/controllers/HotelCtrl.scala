package controllers.web

import java.text.SimpleDateFormat
import java.time.Period
import javax.inject._

import com.alajobi.ota.utils.{ Pagination, PaginationAbstract, CachedVariables }
import crypto.{ Encrypt, TransRefAlgorithm }
import com.alajobi.ota.flights.HttpRequestParser
import com.hotels._
import org.joda.time.LocalDate
import play.api.i18n.MessagesApi
import play.api.libs.json.{ JsNumber, JsObject, JsString }
import play.api.mvc._
import mailer.MailService
import utils.SupplierConfigManager
import utils.silhouette.{ MyEnv, WebController }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import akka.actor.Props
import api.ServicesAPI
import com.mohiva.play.silhouette.api.Silhouette
import hotel.dto.entity._
import models._
import views.html.web._
import hotel.dto.entity.booking._
import com.alajobi.ota.utils.SupplierManager.Implicits._
import com.alajobi.ota.utils.SupplierManager.{ Implicits => HotelConfig }
import java._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class HotelCtrl @Inject() (val silhouette: Silhouette[MyEnv[Users]], val messagesApi: MessagesApi, serviceAPI: ServicesAPI, fareRuleService: HotelFareRuleService, hotelCRUD: HotelCRUD, implicit val encrypt: Encrypt, implicit val mailService: MailService) extends WebController {

  implicit val cache = serviceAPI.cache
  lazy implicit val supplier = hotelSupplier

  val hotelBookingForm = HotelBookingForm.travellerForm

  def getSearchResponse(implicit request: Request[_]): HotelSearchResponse = cache.getItem[HotelSearchResponse](CachedVariables.hotelResultKey).orNull

  def noResult = UserAwareAction(implicit request => Ok(hotels.error()))

  def search = UserAwareAction.async { implicit request =>
    val searchRequest = HttpRequestParser.hotelSearch(supplier.getName)
    for {
      apiHttpResponse <- serviceAPI.hotelApi.performHotelSearch(searchRequest)
      hotelResponse <- fareRuleService(apiHttpResponse)
      _ <- Future.successful(cache.setItem(CachedVariables.hotelResultKey, hotelResponse))
    } yield Ok(JsObject(Map(
      "responseCode" -> JsNumber(if (apiHttpResponse.getHotels.nonEmpty) 200 else 500)
    )))

  }

  def result(page: Int, perPage: Int) = UserAwareAction { implicit request =>
    if (getSearchResponse != null) {
      val totalSize = getSearchResponse.getHotels.size()
      val pagination = new Pagination(PaginationAbstract(getSearchResponse.getHotels, perPage, page, totalSize))
      Ok(hotels.result(pagination.paginate, getSearchResponse.getSearchRequest))
    } else
      Redirect(controllers.web.routes.HotelCtrl.noResult())
  }

  def checkAvailability(hotelId: String, fl_tabHash: String, async: Boolean) = UserAwareAction.async { implicit request =>
    if (getSearchResponse != null) {
      val searchRequest = getSearchResponse.getSearchRequest.getHotelAvailabilityRequest
      searchRequest.setHotelId(List[String](encrypt.decrypt(hotelId)))
      serviceAPI.hotelApi.performHotelAvailability(searchRequest).map(availResponse => if (async)
        Ok(JsObject(Map("status" -> JsNumber(if (availResponse.getHotels.size() > 0) 200 else 500), "response" -> JsString("" + play.libs.Json.toJson(availResponse)))))
      else Ok(hotels.detail(getSearchResponse.getHotels.find(_.getHotelId.equalsIgnoreCase(encrypt.decrypt(hotelId))).get, getSearchResponse.getSearchRequest)))
    } else Future.successful(Ok(JsObject(Map("status" -> JsNumber(502)))))

  }

  def bookRoomTraveller(rix: String, hid: String, fl_tabHash: String) = UserAwareAction { implicit request =>
    if (getSearchResponse != null) {
      val selectedHotel = getSearchResponse.getHotels.find(_.getHotelId.equalsIgnoreCase(encrypt.decrypt(hid))).get
      val selectedRoom = selectedHotel.getRooms.find(a => a.getRooms.head.getRoomRateId.equals(encrypt.decrypt(rix))).get.getRooms.head
      Ok(hotels.traveller(selectedHotel, selectedRoom, getSearchResponse.getSearchRequest, fl_tabHash, hotelBookingForm))
    } else Redirect(controllers.web.routes.HotelCtrl.noResult())
  }

  private def parseTravellers(post: Map[String, Seq[String]]): List[TravellerRequest] = {
    val noOfRooms = post("roomCount").head.toInt
    var travellers = List[TravellerRequest]()
    for (roomIndex <- 1 to noOfRooms) {
      for (paxId <- 1 to post(s"room${roomIndex}_adtCount").head.toInt) {
        val ptype = post(s"adt_room_${roomIndex}_pax_${paxId}_passengerType").head
        val title = post(s"adt_room_${roomIndex}_pax_${paxId}_title").head
        val fname = post(s"adt_room_${roomIndex}_pax_${paxId}_firstName").head
        val oname = post(s"adt_room_${roomIndex}_pax_${paxId}_otherName").headOption
        val lname = post(s"adt_room_${roomIndex}_pax_${paxId}_lastName").head
        val age = None
        val roomid = post(s"adt_room_${roomIndex}_pax_${paxId}_title").head
        travellers ::= TravellerRequest(title, fname, oname, lname, ptype, age, roomid, roomIndex)
      }
      for (paxId <- 1 to post(s"room${roomIndex}_chdCount").head.toInt) {
        val ptype = post(s"chd_room_${roomIndex}_pax_${paxId}_passengerType").head
        val title = post(s"chd_room_${roomIndex}_pax_${paxId}_title").head
        val fname = post(s"chd_room_${roomIndex}_pax_${paxId}_firstName").head
        val oname = post(s"chd_room_${roomIndex}_pax_${paxId}_otherName").headOption
        val lname = post(s"chd_room_${roomIndex}_pax_${paxId}_lastName").head
        val age = Option(post(s"chd_room_${roomIndex}_pax_${paxId}_age").head.toInt)
        val roomid = post(s"chd_room_${roomIndex}_pax_${paxId}_title").head
        travellers ::= TravellerRequest(title, fname, oname, lname, ptype, age, roomid, roomIndex)
      }
    }
    travellers
  }

  def postBookRoom(hotelId: String, roomRateId: String, fl_tabHash: String) = UserAwareAction.async { implicit request =>
    val selectedHotel = getSearchResponse.getHotels.find(_.getHotelId.equalsIgnoreCase(encrypt.decrypt(hotelId))).get
    val selectedRoom = selectedHotel.getRooms.find(a => a.getRooms.head.getRoomRateId.equals(roomRateId)).get
    val searchRequest = getSearchResponse.getSearchRequest

    /**
     * Reparse the form to input value that weren't present.
     */
    val submitted = hotelBookingForm.bindFromRequest().get
    val travellers = parseTravellers(request.body.asFormUrlEncoded.get)
    val formResponse = hotelBookingForm.bindFromRequest().fill(HotelBookingRequest(travellers, submitted.remarks, submitted.paymentMethodId, submitted.salesCategory, submitted.hotelId, submitted.contactTitle, submitted.contactFirstName, submitted.contactLastName, submitted.email, submitted.phone))

    formResponse.fold(
      hasError => Future.successful(Ok(hotels.traveller(selectedHotel, selectedRoom.getRooms.head, searchRequest, fl_tabHash, formResponse)).withNewSession.flashing(("error", "Please fill the required field(s)"))),
      onSuccess => {
        val bookingRequest = parseBookingRequest(selectedHotel, searchRequest, onSuccess, roomRateId, selectedRoom)
        bookingRequest.getOrder.setSign("confirm")
        bookingRequest.getOrder.setUserId(request.identity.asInstanceOf[Option[Users]].getOrElse(new Users).getId)
        for {
          wsResponse <- serviceAPI.hotelApi.performHotelBooking(bookingRequest)
          response <- fareRuleService(wsResponse, bookingRequest)
          savedDb <- hotelCRUD.create(response, onSuccess)
          _ <- utils.Mailer.hotelBooking(savedDb, response)
          result <- Future(if (savedDb.getStatus.equals(BookingStatus.CONFIRMED)) Redirect(routes.HotelCtrl.confirmation(BookingStatus.CONFIRMED.name().toLowerCase, encrypt.encrypt(savedDb.getId.toString))) else Redirect(routes.HotelCtrl.confirmation(savedDb.getStatus.name.toLowerCase, encrypt.encrypt(savedDb.getId.toString))))
        } yield result
      }
    )
  }

  def confirmation(status: String, bookingId: String) = UserAwareAction { implicit request =>
    val hotelBooking = Bookings.find.byId(java.lang.Long.parseLong(encrypt.decrypt(bookingId)))
    if (hotelBooking.getStatus.equals(BookingStatus.CONFIRMED))
      Ok(hotels.successBooking(hotelBooking.getHotelBookings))
    else
      Ok(hotels.failedBooking())
  }

  private def parseBookingRequest(hotel: Hotel, searchRequest: SearchRequest, hotelBookingRequest: HotelBookingRequest, roomRateId: String, hotelRooms: HotelRooms) = {
    val bookingRequest = new BookingHotelRequest
    val order = new Order
    val orderPrice = hotelRooms.getRooms.head.getOrderPrice
    orderPrice.setHotelFareRulesId(hotel.getRooms.head.getRooms.head.getOrderPrice.getHotelFareRulesId)
    order.setOrderPrice(orderPrice)
    order.setOrderId(java.lang.Long.parseLong(TransRefAlgorithm.getUniqueRefLong().getRefCode))
    order.setCountryCode(HotelConfig.hotelSupplierConfig("supplier.country").configValue)
    order.setSalesCategory(hotelBookingRequest.salesCategory)
    val ckin = searchRequest.getHotelAvailabilityRequest.getCheckIn
    val ckout = searchRequest.getHotelAvailabilityRequest.getCheckOut
    val dateFormat = new SimpleDateFormat(HotelConfig.hotelSupplierConfig("supplier.dateformat").configValue)
    try {
      order.setCheckIn(dateFormat.parse(ckin))
      order.setCheckOut(dateFormat.parse(ckout))
    } catch {
      case e: Exception => e.printStackTrace()
    }
    order.setCityCode(searchRequest.getHotelAvailabilityRequest.getCityId)
    order.setPaymentId(lang.Long.parseLong(hotelBookingRequest.paymentMethodId))
    order.setRemark(hotelBookingRequest.remarks.getOrElse(""))
    order.setSupplierId(HotelConfig.hotelSupplier.getName)
    bookingRequest.setOrder(order)
    var i = 0
    bookingRequest.setHotelRooms(searchRequest.getHotelAvailabilityRequest.getRooms.map { room =>
      val agentHotelRoom = new AgentHotelRoom
      agentHotelRoom.setHotelCode(hotelBookingRequest.hotelId)
      agentHotelRoom.setHotelName(hotelRooms.getRooms.head.getRoomName)
      agentHotelRoom.setAdultsQuantity(searchRequest.getHotelAvailabilityRequest.getRooms.get(i).getAdults)
      agentHotelRoom.setChildrenQuantity(searchRequest.getHotelAvailabilityRequest.getRooms.get(i).getChildren)
      val travellers = hotelBookingRequest.travellers.filter(_.roomIndex.equals(i + 1)).map { o =>
        val traveller = new Traveller
        agentHotelRoom.setRoomCode(o.roomId)
        agentHotelRoom.setRoomRateId(roomRateId)
        agentHotelRoom.setRoomRateCode(roomRateId)
        agentHotelRoom.setHotelRoom(hotelRooms.getRooms.head)
        traveller.setTitle(o.title)
        traveller.setFullName(s"${o.lastName} ${o.firstName}")
        traveller.setFirstName(o.firstName)
        traveller.setMiddleName(o.otherName.orNull)
        traveller.setLastName(o.lastName)
        traveller.setAgeCategory(o.passengerType)
        traveller.setType(if (o.passengerType.equalsIgnoreCase("ADT")) "0" else "1")
        if (o.passengerType.equalsIgnoreCase("CHD")) {
          traveller.setAge(o.age.get)
        }
        traveller
      }
      agentHotelRoom.setTravellerList(travellers)
      i += 1
      agentHotelRoom
    }.asJava)
    bookingRequest
  }

}

