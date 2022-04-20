package controllers.admin.systems

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import crypto.Encrypt
import models.{ EmailTemplates, PrivateUsers, Roles, SmsTemplates }
import play.api.data.Form
import models._
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json.{ JsNumber, JsObject, JsString }
import utils.silhouette.MyEnv
import utils.silhouette.admin.{ AdminController, WithRoles }
import views.html.admin.systems._

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._

/**
 * Created by
 * Igbalajobi Jamiu O. on 01/06/2016 6:44 PM.
 */

case class Airline(id: Option[Long], name: String, code: String)

case class Airport(id: Option[Long], code: String, name: String, city: String, state: String, country: Long)

case class Country(id: Option[Long], name: String, isoCode2: String, isoCode3: String)

case class Facility(id: Option[Long], name: String, fontCode: Option[String])

class StaticDataCRUDCtrl @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi, implicit val encrypt: Encrypt) extends AdminController {

  val roles = WithRoles(Roles.operation_manager.name(), Roles.admin.name())

  val airlineForm = Form(mapping(
    "id" -> optional(longNumber),
    "name" -> nonEmptyText,
    "code" -> nonEmptyText.verifying("Only 2 characters are allowed", a => a.length == 2)
  )(Airline.apply)(Airline.unapply))

  val airportForm = Form(mapping(
    "id" -> optional(longNumber),
    "code" -> nonEmptyText.verifying("Only 3 characters are allowed", a => a.length == 3),
    "name" -> nonEmptyText,
    "city" -> nonEmptyText,
    "state" -> nonEmptyText,
    "country" -> longNumber
  )(Airport.apply)(Airport.unapply))

  val countryForm = Form(mapping(
    "id" -> optional(longNumber),
    "name" -> nonEmptyText,
    "isoCode2" -> nonEmptyText.verifying("Only 2 characters are allowed", a => a.length == 2),
    "isoCode3" -> nonEmptyText.verifying("Only 2 characters are allowed", a => a.length == 3)
  )(Country.apply)(Country.unapply))

  val facilityForm = Form(mapping(
    "id" -> optional(longNumber),
    "name" -> nonEmptyText,
    "fontCode" -> optional(nonEmptyText)
  )(Facility.apply)(Facility.unapply))

  def airlines = SecuredAction(roles) { implicit request =>
    Ok(staticCrud.airline(Airlines.find.all().asScala.toList))
  }

  def createAirline(id: Long) = SecuredAction(roles) { implicit request =>
    if (id == 0) Ok(staticCrud.createAirline(airlineForm))
    else {
      val db = Airlines.find.byId(id)
      Ok(staticCrud.createAirline(airlineForm.fill(Airline(Option(id), db.getName, db.getAirlineCode))))
    }
  }

  def saveAirline = SecuredAction(roles) { implicit request =>
    airlineForm.bindFromRequest().fold(
      hasError => BadRequest(staticCrud.createAirline(hasError)),
      formData => {
        val airline = new Airlines
        airline.setAirlineCode(formData.code)
        airline.setName(formData.name)
        formData.id match {
          case Some(_) =>
            airline.setId(formData.id.get)
            airline.update()
          case _ =>
            airline.insert()
        }
        Redirect(routes.StaticDataCRUDCtrl.airlines()).withNewSession.flashing(("success", "Airline Saved Successfully"))
      }
    )
  }

  def airports = SecuredAction(roles) { implicit request =>
    Ok(staticCrud.airport(Airports.find.all().asScala.toList))
  }

  def createAirport(id: Long) = SecuredAction(roles) { implicit request =>
    if (id == 0) Ok(staticCrud.createAirport(airportForm))
    else {
      val db = Airports.find.byId(id)
      Ok(staticCrud.createAirport(airportForm.fill(Airport(Option(db.getId), db.getAirCode, db.getAirportName, db.getCity, db.getState, db.getCountryId.getId))))
    }
  }

  def saveAirport = SecuredAction(roles) { implicit request =>
    airportForm.bindFromRequest().fold(
      hasError => BadRequest(staticCrud.createAirport(hasError)),
      formData => {
        val airport = new Airports
        airport.setAirCode(formData.code)
        airport.setAirportName(formData.name)
        airport.setCity(formData.city)
        airport.setState(formData.state)
        val country = Countries.find.byId(formData.country)
        airport.setCountryCode(country.getIsoCode2)
        airport.setCountryName(country.getName)
        airport.setCountryId(country)
        formData.id match {
          case Some(_) =>
            airport.setId(formData.id.get)
            airport.update()
          case _ => airport.insert()
        }
        Redirect(routes.StaticDataCRUDCtrl.airports()).withNewSession.flashing(("success", "Airport Saved Successfully"))
      }
    )
  }

  def country = SecuredAction(roles) { implicit request =>
    Ok(staticCrud.country(Countries.find.all().asScala.toList))
  }

  def createCountry(id: Long) = SecuredAction(roles) { implicit request =>
    if (id == 0) Ok(staticCrud.createCountry(countryForm))
    else {
      val db = Countries.find.byId(id)
      Ok(staticCrud.createCountry(countryForm.fill(Country(Option(db.getId), db.getName, db.getIsoCode2, db.getIsoCode3))))
    }
  }

  def saveCountry = SecuredAction(roles) { implicit request =>
    countryForm.bindFromRequest().fold(
      hasError => BadRequest(staticCrud.createCountry(hasError)),
      formData => {
        val airport = new Countries
        airport.setName(formData.name)
        airport.setIsoCode2(formData.isoCode2)
        airport.setIsoCode3(formData.isoCode3)
        formData.id match {
          case Some(_) =>
            airport.setId(formData.id.get)
            airport.update()
          case _ => airport.insert()
        }
        Redirect(routes.StaticDataCRUDCtrl.country()).withNewSession.flashing(("success", "Country Saved Successfully"))
      }
    )
  }

  def facilities = SecuredAction(roles) { implicit request =>
    Ok(staticCrud.facilities(Facilities.find.all().asScala.toList))
  }

  def createFacility(id: Long) = SecuredAction(roles) { implicit request =>
    if (id == 0) Ok(staticCrud.createFacility(facilityForm))
    else {
      val db = Facilities.find.byId(id)
      Ok(staticCrud.createFacility(facilityForm.fill(Facility(Option(db.getId), db.getName, Option(db.getFontCode)))))
    }
  }

  def saveFacility = SecuredAction(roles) { implicit request =>
    facilityForm.bindFromRequest().fold(
      hasError => BadRequest(staticCrud.createFacility(hasError)),
      formData => {
        val facilities = new Facilities
        facilities.setName(formData.name)
        facilities.setFontCode(formData.fontCode.orNull)
        formData.id match {
          case Some(_) =>
            facilities.setId(formData.id.get)
            facilities.update()
          case _ => facilities.insert()
        }
        Redirect(routes.StaticDataCRUDCtrl.facilities()).withNewSession.flashing(("success", "Facility Saved Successfully"))
      }
    )
  }
}