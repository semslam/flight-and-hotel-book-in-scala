package controllers.admin.pricerule

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import crypto.Encrypt
import models._
import play.api.data.Form
import play.api.data.Forms._
import views.html.admin._
import utils.silhouette.admin.{ AdminController, WithRoles }
import utils.silhouette.MyEnv
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import java._

import play.api.Play.current
import play.api.i18n.{ MessagesApi, Messages, Lang }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import javax.inject.{ Inject, Singleton }

/**
 * Created by
 * Igbalajobi Jamiu O. on 13/06/2016 9:37 AM.
 */
case class HotelFareRuleDAO(id: Option[String], apiSupplierId: String, name: String, hotelDestinations: Option[List[String]],
  b2cFareDirection: String, b2cDispenseValue: String, b2cValueType: String, b2bFareDirection: String, b2bDispenseValue: String, b2bValueType: String)

class HotelPriceRuleCtrl @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi, encrypt: Encrypt) extends AdminController {

  implicit val enc = this.encrypt

  val roles = WithRoles(Roles.operation_manager.name())

  var formRule = Form(mapping(
    "id" -> optional(nonEmptyText),
    "apiSupplierId" -> nonEmptyText,
    "name" -> nonEmptyText,
    "hotelDestinations" -> optional(list(nonEmptyText)),
    "b2cFareDirection" -> nonEmptyText,
    "b2cDispenseValue" -> nonEmptyText,
    "b2cValueType" -> nonEmptyText,
    "b2bFareDirection" -> nonEmptyText,
    "b2bDispenseValue" -> nonEmptyText,
    "b2bValueType" -> nonEmptyText
  )(HotelFareRuleDAO.apply)(HotelFareRuleDAO.unapply))

  def index = SecuredAction(roles) { implicit request => Ok(hotelrule.index(HotelsFareRules.find.all().asScala.toList)) }

  def create(id: String) = SecuredAction(roles) { implicit request =>
    if (Option(id).nonEmpty) {
      val data = HotelsFareRules.find.byId(lang.Long.parseLong(encrypt.decrypt(id)))
      formRule = formRule.fill(HotelFareRuleDAO(Option(data.getId.toString), data.getSupplier.getId.toString, data.getTitle, Option(data.getHotelFareRuleDestinations.map(_.getId.toString).toList),
        data.getB2cFareDirection, data.getB2cDispenseValue.toString, data.getB2cValueType.name(), data.getB2bFareDirection, data.getB2bDispenseValue.toString, data.getB2bValueType.name()))
    }
    Ok(hotelrule.create(formRule))
  }

  def save = SecuredAction(roles) { implicit request =>
    formRule.bindFromRequest().fold(
      hasError => BadRequest(hotelrule.create(hasError)),
      success => {
        success.id match {
          case Some(_) =>
            val rule = HotelsFareRules.find.byId(lang.Long.parseLong(success.id.get))
            rule.setSupplier(ApiSuppliers.find.byId(lang.Long.parseLong(success.apiSupplierId)))
            rule.setTitle(success.name)
            rule.setActive(true)
            rule.setB2cDispenseValue(lang.Double.parseDouble(success.b2cDispenseValue))
            rule.setB2cFareDirection(success.b2cFareDirection)
            rule.setB2cValueType(ValueTypes.valueOf(success.b2cValueType))
            rule.setB2bDispenseValue(lang.Double.parseDouble(success.b2bDispenseValue))
            rule.setB2bFareDirection(success.b2bFareDirection)
            rule.setB2bValueType(ValueTypes.valueOf(success.b2bValueType))
            rule.update()
          case _ =>
            val rule = new HotelsFareRules
            rule.setTitle(success.name)
            rule.setActive(true)
            rule.setSupplier(ApiSuppliers.find.byId(lang.Long.parseLong(success.apiSupplierId)))
            rule.setB2cDispenseValue(lang.Double.parseDouble(success.b2cDispenseValue))
            rule.setB2cFareDirection(success.b2cFareDirection)
            rule.setB2cValueType(ValueTypes.valueOf(success.b2cValueType))
            rule.setB2bDispenseValue(lang.Double.parseDouble(success.b2bDispenseValue))
            rule.setB2bFareDirection(success.b2bFareDirection)
            rule.setB2bValueType(ValueTypes.valueOf(success.b2bValueType))
            rule.insert()
        }
        Redirect(routes.HotelPriceRuleCtrl.index()).withNewSession.flashing(("success", "Hotel Rule Saved Successfully"))
      }
    )
  }

  def delete(id: String) = SecuredAction(roles) { implicit request =>
    val data = HotelsFareRules.find.byId(java.lang.Long.parseLong(encrypt.decrypt(id)))
    if (data != null) {
      data.setActive(false)
      data.update()
      data.delete()
      Redirect(routes.HotelPriceRuleCtrl.index).withSession(("success", "Fare Rule Deleted Successfully"))
    } else {
      Redirect(routes.HotelPriceRuleCtrl.index).withSession(("error", "Request failed. Could not delete item"))
    }
  }

}
