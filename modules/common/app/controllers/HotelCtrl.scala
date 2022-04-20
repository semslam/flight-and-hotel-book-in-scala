package controllers.common

import java._
import java.text.SimpleDateFormat
import javax.inject._

import api.ServicesAPI
import com.alajobi.ota.flights.HttpRequestParser
import com.alajobi.ota.utils.SupplierManager.Implicits._
import com.alajobi.ota.utils.SupplierManager.{ Implicits => HotelConfig }
import com.alajobi.ota.utils.{ CachedVariables, Pagination, PaginationAbstract }
import com.hotels._
import com.mohiva.play.silhouette.api.Silhouette
import crypto.{ Encrypt, TransRefAlgorithm }
import hotel.dto.entity._
import hotel.dto.entity.booking._
import mailer.MailService
import models._
import play.api.i18n.MessagesApi
import play.api.libs.json.{ JsNumber, JsObject, JsString }
import play.api.mvc._
import utils.silhouette.MyEnv

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HotelCtrl @Inject() (val messagesApi: MessagesApi, serviceAPI: ServicesAPI, fareRuleService: HotelFareRuleService, hotelCRUD: HotelCRUD, implicit val encrypt: Encrypt, implicit val mailService: MailService) extends Controller {

  implicit val cache = serviceAPI.cache
  lazy implicit val supplier = hotelSupplier

  def search = Action.async { implicit request =>
    val searchRequest = HttpRequestParser.hotelSearch(supplier.getName)
    for {
      apiHttpResponse <- serviceAPI.hotelApi.performHotelSearch(searchRequest)
      hotelResponse <- fareRuleService(apiHttpResponse)
      _ <- Future.successful(cache.setItem(CachedVariables.hotelResultKey, hotelResponse))
    } yield Ok(JsObject(Map(
      "responseCode" -> JsNumber(if (apiHttpResponse.getHotels.nonEmpty) 200 else 500)
    )))

  }

}

