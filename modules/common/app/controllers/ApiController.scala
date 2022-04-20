package controllers.common

import javax.inject.{ Inject, Singleton }

import com.avaje.ebean.Expr
import models.{ Airports, HotelDestinations }
import play.api.libs.json.{ util => _, _ }
import play.api.mvc._
import play.cache.Cached
import play.api.libs.json._
import play.api.libs.functional.syntax._
import java._
import java.util.Comparator
import com.alajobi.ota.utils.SupplierManager._
import api.ServicesAPI

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

/**
 * Created by Igbalajobi Jamiu Okunade on 4/15/17.
 */
@Singleton
class ApiController @Inject() (serviceAPI: ServicesAPI) extends Controller {

  def suggestAirport(key: String) = Action { implicit request =>
    @Cached(key = "airportSuggest")
    val flightResult = Airports.find.where().raw(s" air_code = '$key' or airport_name like '%$key%' or state like '%$key%' or country_name like '%$key%' order by case when `air_code` = '$key' then 1 else 2 end, air_code ").findList()
    implicit val airportsWrites: Writes[AirportSuggest] = (
      (JsPath \ "state").write[String] and
      (JsPath \ "city").write[String] and
      (JsPath \ "airCode").write[String] and
      (JsPath \ "airportName").write[String] and
      (JsPath \ "countryName").write[String] and
      (JsPath \ "countryCode").write[String] and
      (JsPath \ "id").write[Long]
    )(unlift(AirportSuggest.unapply))
    Ok(Json.toJson(AirportSuggest.list(flightResult.asScala.toList)))
  }

  def suggestCity(key: String) = Action { implicit request =>
    @Cached(key = "hotelSuggest")
    val query = HotelDestinations.find.where().raw(s"supplier.name = '$hotelSupplierName' and name like '%$key%' or country.name like '%$key%'")
    val hotelResult = query.findList()
    implicit val airportsWrites: Writes[HotelSuggest] = (
      (JsPath \ "code").write[String] and
      (JsPath \ "destinationType").write[String] and
      (JsPath \ "name").write[String] and
      (JsPath \ "countryName").write[String] and
      (JsPath \ "id").write[Long]
    )(unlift(HotelSuggest.unapply))
    Ok(Json.toJson(HotelSuggest.list(hotelResult.asScala.toList)))
  }

}

case class AirportSuggest(state: String, city: String, airCode: String, airportName: String, countryName: String, countryCode: String, id: Long)

case class HotelSuggest(code: String, destinationType: String, name: String, countryName: String, id: Long)

object AirportSuggest {
  def list(airport: List[Airports]) = airport.map(a => AirportSuggest(a.getState, a.getCity, a.getAirCode, a.getAirportName, a.getCountryName, a.getCountryCode, a.getId))
}

object HotelSuggest {
  def list(city: List[HotelDestinations]) = city.map(a => HotelSuggest(a.getCode, a.getDestinationType, a.getName, a.getCountry.getName, a.getId))
}
