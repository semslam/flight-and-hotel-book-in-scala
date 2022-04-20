package controllers.admin.pricerule

import javax.inject.{ Inject, Singleton }

import com.mohiva.play.silhouette.api.Silhouette
import crypto.Encrypt
import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import utils.silhouette.admin.{ AdminController, WithRoles }
import utils.silhouette.MyEnv
import views.html.admin._

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import scala.reflect.macros.blackbox

/**
 * Created by
 * Igbalajobi Jamiu O. on 13/06/2016 9:37 AM.
 */
case class B2CFareRule(id: Option[String], sotiFareDirection: String, sotiFareOption: String, sotiDispenseValue: String, sotiValueType: String, sitiFareDirection: String, sitiFareOption: String, sitiDispenseValue: String, sitiValueType: String, sotoFareDirection: String, sotoFareOption: String, sotoDispenseValue: String, sotoValueType: String)

case class B2BFareRule(id: Option[String], sotiFareDirection: String, sotiFareOption: String, sotiDispenseValue: String, sotiValueType: String, sitiFareDirection: String, sitiFareOption: String, sitiDispenseValue: String, sitiValueType: String, sotoFareDirection: String, sotoFareOption: String, sotoDispenseValue: String, sotoValueType: String)

case class FlightFareRuleDAO(id: Option[String], ticketClass: List[String], tripType: List[String], destinationLocale: List[String], isCommissionable: Boolean, airlineCommissionableId: String, name: String, description: Option[String], b2bFareRule: B2BFareRule, b2cFareRule: B2CFareRule, disclaimerTitle: Option[String], disclaimerContent: Option[String], destinationAirports: Option[List[String]])

case class AirlineBlacklistDAO(id: Option[String], name: String, code: String)

@Singleton
class FlightPriceRuleCtrl @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi, encrypt: Encrypt, sqlQueryCached: SqlQueryCached) extends AdminController {
  implicit val enc = this.encrypt

  val roles = WithRoles(Roles.operation_manager.name())

  val createCommissionableForm = Form(tuple(
    "id" -> ignored(None: Option[String]),
    "airlineId" -> nonEmptyText
  ))

  val airlineBlacklistDaoForm = Form(mapping(
    "id" -> optional(nonEmptyText),
    "name" -> nonEmptyText,
    "code" -> nonEmptyText
  )(AirlineBlacklistDAO.apply)(AirlineBlacklistDAO.unapply))

  val fareRuleForm = Form(mapping(
    "id" -> optional(nonEmptyText),
    "ticketClass" -> list(nonEmptyText),
    "tripType" -> list(nonEmptyText),
    "destinationLocale" -> list(nonEmptyText),
    "isCommissionable" -> boolean,
    "airlineCommissionableId" -> nonEmptyText,
    "name" -> nonEmptyText,
    "description" -> optional(nonEmptyText),
    "b2bFareRule" -> mapping(
      "id" -> optional(nonEmptyText),
      "sotiFareDirection" -> nonEmptyText,
      "sotiFareOption" -> nonEmptyText,
      "sotiDispenseValue" -> nonEmptyText,
      "sotiValueType" -> nonEmptyText,
      "sitiFareDirection" -> nonEmptyText,
      "sitiFareOption" -> nonEmptyText,
      "sitiDispenseValue" -> nonEmptyText,
      "sitiValueType" -> nonEmptyText,
      "sotoFareDirection" -> nonEmptyText,
      "sotoFareOption" -> nonEmptyText,
      "sotoDispenseValue" -> nonEmptyText,
      "sotoValueType" -> nonEmptyText
    )(B2BFareRule.apply)(B2BFareRule.unapply),
    "b2cFareRule" -> mapping(
      "id" -> optional(nonEmptyText),
      "sotiFareDirection" -> nonEmptyText,
      "sotiFareOption" -> nonEmptyText,
      "sotiDispenseValue" -> nonEmptyText,
      "sotiValueType" -> nonEmptyText,
      "sitiFareDirection" -> nonEmptyText,
      "sitiFareOption" -> nonEmptyText,
      "sitiDispenseValue" -> nonEmptyText,
      "sitiValueType" -> nonEmptyText,
      "sotoFareDirection" -> nonEmptyText,
      "sotoFareOption" -> nonEmptyText,
      "sotoDispenseValue" -> nonEmptyText,
      "sotoValueType" -> nonEmptyText
    )(B2CFareRule.apply)(B2CFareRule.unapply),
    "disclaimerTitle" -> optional(nonEmptyText),
    "disclaimerContent" -> optional(nonEmptyText),
    "destinationAirports" -> optional(list(nonEmptyText))
  )(FlightFareRuleDAO.apply)(FlightFareRuleDAO.unapply))

  def commissionableAirline(id: String) = SecuredAction(roles) { implicit request =>
    if (id != null && !id.isEmpty) {
      val airlinComm = AirlineCommissionable.find.byId(java.lang.Long.parseLong(encrypt.decrypt(id)))
      createCommissionableForm.fill(Some(airlinComm.getId.toString), airlinComm.getAirlineId.getId.toString)
    }
    Ok(pricerule.commissionableAirline(createCommissionableForm, AirlineCommissionable.find.all().asScala.toList, sqlQueryCached.getQueryResults(classOf[Airlines])))
  }

  def deleteCommissionableAirline(id: String) = SecuredAction(roles) { implicit request =>
    val airlinComm = AirlineCommissionable.find.byId(java.lang.Long.parseLong(encrypt.decrypt(id)))
    airlinComm.delete()
    Redirect(routes.FlightPriceRuleCtrl.commissionableAirline()).withNewSession.flashing(("success", "Priority Airline Deleted Successfully."))
  }

  def saveCommissionableAirline = SecuredAction(roles) { implicit request =>
    createCommissionableForm.bindFromRequest().fold(
      error => BadRequest(pricerule.commissionableAirline(error, AirlineCommissionable.find.all().asScala.toList, sqlQueryCached.getQueryResults(classOf[Airlines]))),
      success => {
        val airlineCommissionable = new AirlineCommissionable
        if (success._1.isDefined) {
          airlineCommissionable.setId(java.lang.Long.parseLong(success._1.get))
        }
        val airline = Airlines.find.byId(java.lang.Long.parseLong(success._2))
        airlineCommissionable.setAirlineId(airline)
        airlineCommissionable.setActive(true)
        airlineCommissionable.setAirlineCode(airline.getAirlineCode)
        airlineCommissionable.setAuthUserId(request.identity)
        airlineCommissionable.save()
        Redirect(routes.FlightPriceRuleCtrl.commissionableAirline()).withNewSession.flashing(("success", "Airline Commission Save Successfully."))
      }
    )
  }

  def fareRuleManagement = SecuredAction(roles) { implicit request =>
    val flightsFareRule = FlightsFareRules.find.all().asScala.toList
    Ok(pricerule.fareRuleManagement(flightsFareRule))
  }

  def createFareRuleManagement(id: String) = SecuredAction(roles) { implicit request =>
    if (id != null && !id.isEmpty) {
      val flightsFareRules = FlightsFareRules.find.byId(java.lang.Long.parseLong(encrypt.decrypt(id)))
      val data = FlightFareRuleDAO(
        id = Some(flightsFareRules.getId.toString),
        ticketClass = flightsFareRules.getCabinClasses.split(":::").toList,
        tripType = flightsFareRules.getTripTypes.split(":::").toList,
        destinationLocale = flightsFareRules.getDestinationLocale.split(":::").toList,
        isCommissionable = flightsFareRules.isCommissionable,
        airlineCommissionableId = flightsFareRules.getAirlineCommissionableId.getId.toString,
        name = flightsFareRules.getName,
        description = Some(flightsFareRules.getDescription),
        b2bFareRule = B2BFareRule(
          id = Some(flightsFareRules.getB2bFlightFareRule.getId.toString),
          sitiDispenseValue = flightsFareRules.getB2bFlightFareRule.getSitiDispenseValue.toString,
          sitiFareDirection = flightsFareRules.getB2bFlightFareRule.getSitiFareDirection,
          sitiFareOption = flightsFareRules.getB2bFlightFareRule.getSitiFareOption.name(),
          sitiValueType = flightsFareRules.getB2bFlightFareRule.getSitiValueType.name(),
          sotiDispenseValue = flightsFareRules.getB2bFlightFareRule.getSotiDispenseValue.toString,
          sotiFareDirection = flightsFareRules.getB2bFlightFareRule.getSotiFareDirection,
          sotiFareOption = flightsFareRules.getB2bFlightFareRule.getSotiFareOption.name(),
          sotiValueType = flightsFareRules.getB2bFlightFareRule.getSotiValueType.name(),
          sotoDispenseValue = flightsFareRules.getB2bFlightFareRule.getSotoDispenseValue.toString,
          sotoFareDirection = flightsFareRules.getB2bFlightFareRule.getSotoFareDirection,
          sotoFareOption = flightsFareRules.getB2bFlightFareRule.getSotoFareOption.name(),
          sotoValueType = flightsFareRules.getB2bFlightFareRule.getSotoValueType.name()
        ),
        b2cFareRule = B2CFareRule(
          id = Some(flightsFareRules.getB2cFlightFareRule.getId.toString),
          sitiDispenseValue = flightsFareRules.getB2cFlightFareRule.getSitiDispenseValue.toString,
          sitiFareDirection = flightsFareRules.getB2cFlightFareRule.getSitiFareDirection,
          sitiFareOption = flightsFareRules.getB2cFlightFareRule.getSitiFareOption.name(),
          sitiValueType = flightsFareRules.getB2cFlightFareRule.getSitiValueType.name(),
          sotiDispenseValue = flightsFareRules.getB2cFlightFareRule.getSotiDispenseValue.toString,
          sotiFareDirection = flightsFareRules.getB2cFlightFareRule.getSotiFareDirection,
          sotiFareOption = flightsFareRules.getB2cFlightFareRule.getSotiFareOption.name(),
          sotiValueType = flightsFareRules.getB2cFlightFareRule.getSotiValueType.name(),
          sotoDispenseValue = flightsFareRules.getB2cFlightFareRule.getSotoDispenseValue.toString,
          sotoFareDirection = flightsFareRules.getB2cFlightFareRule.getSotoFareDirection,
          sotoFareOption = flightsFareRules.getB2cFlightFareRule.getSotoFareOption.name(),
          sotoValueType = flightsFareRules.getB2cFlightFareRule.getSotoValueType.name()
        ),
        disclaimerTitle = Option(flightsFareRules.getDisclaimerTitle),
        disclaimerContent = Option(flightsFareRules.getDisclaimerContent),
        destinationAirports = Option(flightsFareRules.getFareRuleAirports.asScala.toList.map(_.getAirportCode))
      )
      Ok(pricerule.createFareRuleManagement(fareRuleForm.fill(data)))
    } else {
      Ok(pricerule.createFareRuleManagement(fareRuleForm))
    }
  }

  def createNonCommissionFareRule(id: String) = SecuredAction(roles) { implicit request =>
    if (id != null && !id.isEmpty) {
      val flightsFareRules = FlightsFareRules.find.byId(java.lang.Long.parseLong(encrypt.decrypt(id)))
      val data = FlightFareRuleDAO(
        id = Some(flightsFareRules.getId.toString),
        ticketClass = flightsFareRules.getCabinClasses.split(":::").toList,
        tripType = flightsFareRules.getTripTypes.split(":::").toList,
        destinationLocale = flightsFareRules.getDestinationLocale.split(":::").toList,
        isCommissionable = flightsFareRules.isCommissionable,
        airlineCommissionableId = "0",
        name = flightsFareRules.getName,
        description = Some(flightsFareRules.getDescription),
        b2bFareRule = B2BFareRule(
          id = Some(flightsFareRules.getB2bFlightFareRule.getId.toString),
          sitiDispenseValue = flightsFareRules.getB2bFlightFareRule.getSitiDispenseValue.toString,
          sitiFareDirection = flightsFareRules.getB2bFlightFareRule.getSitiFareDirection,
          sitiFareOption = flightsFareRules.getB2bFlightFareRule.getSitiFareOption.name(),
          sitiValueType = flightsFareRules.getB2bFlightFareRule.getSitiValueType.name(),
          sotiDispenseValue = flightsFareRules.getB2bFlightFareRule.getSotiDispenseValue.toString,
          sotiFareDirection = flightsFareRules.getB2bFlightFareRule.getSotiFareDirection,
          sotiFareOption = flightsFareRules.getB2bFlightFareRule.getSotiFareOption.name(),
          sotiValueType = flightsFareRules.getB2bFlightFareRule.getSotiValueType.name(),
          sotoDispenseValue = flightsFareRules.getB2bFlightFareRule.getSotoDispenseValue.toString,
          sotoFareDirection = flightsFareRules.getB2bFlightFareRule.getSotoFareDirection,
          sotoFareOption = flightsFareRules.getB2bFlightFareRule.getSotoFareOption.name(),
          sotoValueType = flightsFareRules.getB2bFlightFareRule.getSotoValueType.name()
        ),
        b2cFareRule = B2CFareRule(
          id = Some(flightsFareRules.getB2cFlightFareRule.getId.toString),
          sitiDispenseValue = flightsFareRules.getB2cFlightFareRule.getSitiDispenseValue.toString,
          sitiFareDirection = flightsFareRules.getB2cFlightFareRule.getSitiFareDirection,
          sitiFareOption = flightsFareRules.getB2cFlightFareRule.getSitiFareOption.name(),
          sitiValueType = flightsFareRules.getB2cFlightFareRule.getSitiValueType.name(),
          sotiDispenseValue = flightsFareRules.getB2cFlightFareRule.getSotiDispenseValue.toString,
          sotiFareDirection = flightsFareRules.getB2cFlightFareRule.getSotiFareDirection,
          sotiFareOption = flightsFareRules.getB2cFlightFareRule.getSotiFareOption.name(),
          sotiValueType = flightsFareRules.getB2cFlightFareRule.getSotiValueType.name(),
          sotoDispenseValue = flightsFareRules.getB2cFlightFareRule.getSotoDispenseValue.toString,
          sotoFareDirection = flightsFareRules.getB2cFlightFareRule.getSotoFareDirection,
          sotoFareOption = flightsFareRules.getB2cFlightFareRule.getSotoFareOption.name(),
          sotoValueType = flightsFareRules.getB2cFlightFareRule.getSotoValueType.name()
        ),
        disclaimerTitle = Option(flightsFareRules.getDisclaimerTitle),
        disclaimerContent = Option(flightsFareRules.getDisclaimerContent),
        destinationAirports = Option(flightsFareRules.getFareRuleAirports.asScala.toList.map(_.getAirportCode))
      )
      Ok(pricerule.createFareRuleManagement(fareRuleForm.fill(data), true))
    } else {
      Ok(pricerule.createFareRuleManagement(fareRuleForm, true))
    }
  }

  def saveFareRule = SecuredAction(roles) { implicit request =>
    fareRuleForm.bindFromRequest().fold(
      error => BadRequest(pricerule.createFareRuleManagement(error)).withNewSession.flashing(("error", "Invalid form input, please check your input and try again.")),
      value => {
        val (flightsPriceRule, b2c, b2b) = value.id match {
          case Some(_) =>
            //For Update
            val rule = FlightsFareRules.find.byId(java.lang.Long.parseLong(value.id.get))
            (rule, B2CFlightFareRules.find.byId(rule.getB2cFlightFareRule.getId), B2BFlightFareRules.find.byId(rule.getB2bFlightFareRule.getId))
          case _ =>
            //For New Record
            (new FlightsFareRules, new B2CFlightFareRules, new B2BFlightFareRules)
        }
        //b2c_siti
        b2c.setSitiDispenseValue(java.lang.Double.parseDouble(value.b2cFareRule.sitiDispenseValue))
        b2c.setSitiFareDirection(value.b2cFareRule.sitiFareDirection)
        b2c.setSitiFareOption(GdsFareOptions.valueOf(value.b2cFareRule.sitiFareOption))
        b2c.setSitiValueType(ValueTypes.valueOf(value.b2cFareRule.sitiValueType))
        //b2c_soti
        b2c.setSotiDispenseValue(java.lang.Double.parseDouble(value.b2cFareRule.sotiDispenseValue))
        b2c.setSotiFareDirection(value.b2cFareRule.sotiFareDirection)
        b2c.setSotiFareOption(GdsFareOptions.valueOf(value.b2cFareRule.sotiFareOption))
        b2c.setSotiValueType(ValueTypes.valueOf(value.b2cFareRule.sotiValueType))
        //b2c_soto
        b2c.setSotoDispenseValue(java.lang.Double.parseDouble(value.b2cFareRule.sotoDispenseValue))
        b2c.setSotoFareDirection(value.b2cFareRule.sotoFareDirection)
        b2c.setSotoFareOption(GdsFareOptions.valueOf(value.b2cFareRule.sotoFareOption))
        b2c.setSotoValueType(ValueTypes.valueOf(value.b2cFareRule.sotoValueType))

        //b2b_siti
        b2b.setSitiDispenseValue(java.lang.Double.parseDouble(value.b2bFareRule.sitiDispenseValue))
        b2b.setSitiFareDirection(value.b2bFareRule.sitiFareDirection)
        b2b.setSitiFareOption(GdsFareOptions.valueOf(value.b2bFareRule.sitiFareOption))
        b2b.setSitiValueType(ValueTypes.valueOf(value.b2bFareRule.sitiValueType))
        //b2b_soti
        b2b.setSotiDispenseValue(java.lang.Double.parseDouble(value.b2bFareRule.sotiDispenseValue))
        b2b.setSotiFareDirection(value.b2bFareRule.sotiFareDirection)
        b2b.setSotiFareOption(GdsFareOptions.valueOf(value.b2bFareRule.sotiFareOption))
        b2b.setSotiValueType(ValueTypes.valueOf(value.b2bFareRule.sotiValueType))
        //b2b_soto
        b2b.setSotoDispenseValue(java.lang.Double.parseDouble(value.b2bFareRule.sotoDispenseValue))
        b2b.setSotoFareDirection(value.b2bFareRule.sotoFareDirection)
        b2b.setSotoFareOption(GdsFareOptions.valueOf(value.b2bFareRule.sotoFareOption))
        b2b.setSotoValueType(ValueTypes.valueOf(value.b2bFareRule.sotoValueType))

        var cabinClass = ""
        var tripType = ""
        var destinationLocale = ""
        value.ticketClass.foreach(cabin => cabinClass = cabinClass.concat(cabin + ":::"))
        value.tripType.foreach(trip => tripType = tripType.concat(trip + ":::"))
        value.destinationLocale.foreach(destination => destinationLocale = destinationLocale.concat(destination + ":::"))
        flightsPriceRule.setCabinClasses(cabinClass)
        flightsPriceRule.setTripTypes(tripType)
        flightsPriceRule.setDestinationLocale(destinationLocale)
        flightsPriceRule.setCommissionable(value.isCommissionable)
        val airlineCommissionable = if (value.airlineCommissionableId.equals("0")) null else AirlineCommissionable.find.byId(java.lang.Long.parseLong(value.airlineCommissionableId))
        flightsPriceRule.setAirlineCommissionableId(airlineCommissionable)
        flightsPriceRule.setName(request.body.asFormUrlEncoded.get("name").head)
        flightsPriceRule.setDescription(request.body.asFormUrlEncoded.get("description").head)

        b2b.save()
        b2c.save()
        flightsPriceRule.setB2bFlightFareRule(b2b)
        flightsPriceRule.setB2cFlightFareRule(b2c)

        flightsPriceRule.setDisclaimerTitle(value.disclaimerTitle.orNull)
        flightsPriceRule.setDisclaimerContent(value.disclaimerContent.orNull)

        flightsPriceRule.save()
        //If applicable to destinations only, please specify and save the destination airport code
        if (value.destinationAirports.isDefined) {
          value.destinationAirports.get.foreach { code =>
            //first delete old record
            FlightRuleDestinationAirports.find.where().eq("flightsFareRules", flightsPriceRule).findList.asScala.foreach(_.delete())
            val flightRuleDest = new FlightRuleDestinationAirports
            flightRuleDest.setAirportCode(code)
            flightRuleDest.setFlightsFareRules(flightsPriceRule)
            flightRuleDest.save()
          }
        }
        Redirect(routes.FlightPriceRuleCtrl.fareRuleManagement()).withNewSession.flashing(("success", "Flight Markup/Commission Saved Successfully."))
      }
    )
  }

  def deleteFareRule(id: String) = SecuredAction(roles) { implicit request =>
    val fareRule = FlightsFareRules.find.byId(java.lang.Long.parseLong(encrypt.decrypt(id)))
    fareRule.delete()
    Redirect(routes.FlightPriceRuleCtrl.fareRuleManagement()).withNewSession.flashing(("success", "Fare Rule Deleted."))
  }

  val airlineList = Airlines.find.all().asScala.toList
  val blacklistedList = BlacklistedAirlines.find.all().asScala.toList

  def blacklistAirlines = SecuredAction(roles) { implicit request =>
    Ok(pricerule.blacklistAirline(airlineBlacklistDaoForm, blacklistedList))
  }

  def blacklistAirline = SecuredAction(roles) { implicit request =>
    airlineBlacklistDaoForm.bindFromRequest().fold(
      formError => BadRequest(pricerule.blacklistAirline(airlineBlacklistDaoForm, blacklistedList)),
      formValue => {
        val blacklist = new BlacklistedAirlines
        blacklist.setAirlineCode(formValue.code.toUpperCase)
        blacklist.setAirlineName(formValue.name)
        blacklist.setBlocked(true)
        if (formValue.id.nonEmpty) {
          blacklist.setId(java.lang.Long.parseLong(formValue.id.get))
        }
        if (BlacklistedAirlines.find.where().eq("airlineCode", formValue.code).findRowCount() > 0)
          Redirect(routes.FlightPriceRuleCtrl.blacklistAirlines()).withNewSession.flashing(("error", "Airline already exists on blacklisted list"))
        else
          blacklist.save()
        Redirect(routes.FlightPriceRuleCtrl.blacklistAirlines()).withNewSession.flashing(("success", "Airline Blacklisting Saved Successful."))
      }
    )
  }

  def deleteBlacklist(id: String) = SecuredAction(roles) { implicit request =>
    BlacklistedAirlines.find.byId(java.lang.Long.parseLong(encrypt.decrypt(id))).deletePermanent()
    Redirect(routes.FlightPriceRuleCtrl.blacklistAirline()).withNewSession.flashing(("success", "Status deleted successfully."))
  }

}
