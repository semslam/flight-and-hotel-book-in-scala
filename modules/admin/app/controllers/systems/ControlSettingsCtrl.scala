package controllers.admin.systems

import com.alajobi.ota.utils.SystemControlSetting
import javax.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import crypto.Encrypt
import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.i18n.MessagesApi
import utils.silhouette.MyEnv
import utils.silhouette.admin.{ AdminController, WithRoles }

/**
 * Created by
 * Igbalajobi Jamiu O. on 13/06/2016 9:37 AM.
 */

case class ControlSettings(usd2ngn: String, system_currency: String, webpay_api_fee: String, flights_dip: String, hotels_dip: String, tax_on_commission: Int, economy_service_charge: BigDecimal, business_service_charge: BigDecimal, premium_service_charge: BigDecimal, first_service_charge: BigDecimal, operation_country: String)

class ControlSettingsCtrl @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi, implicit val encrypt: Encrypt) extends AdminController {
  val roles = WithRoles(Roles.admin.name(), Roles.operation_manager.name())
  val form = Form(mapping(
    "usd2ngn" -> nonEmptyText,
    "system_currency" -> nonEmptyText,
    "webpay_api_fee" -> nonEmptyText,
    "flights_dip" -> nonEmptyText,
    "hotels_dip" -> nonEmptyText,
    "tax_on_commission" -> number,
    "economy_service_charge" -> bigDecimal,
    "business_service_charge" -> bigDecimal,
    "premium_service_charge" -> bigDecimal,
    "first_service_charge" -> bigDecimal,
    "operation_country" -> nonEmptyText
  )(ControlSettings.apply)(ControlSettings.unapply))
  lazy val systemControlSetting = SystemControlSetting.getInstance()

  def index = SecuredAction(roles) { implicit request =>
    Ok(views.html.admin.systems.controlSetting(form.fill(ControlSettings(
      usd2ngn = systemControlSetting.get("usd2ngn"),
      system_currency = systemControlSetting.get("system_currency"),
      webpay_api_fee = systemControlSetting.get("webpay_api_fee"),
      flights_dip = systemControlSetting.get("flights_dip"),
      hotels_dip = systemControlSetting.get("hotels_dip"),
      tax_on_commission = systemControlSetting.get("tax_on_commission").toInt,
      economy_service_charge = BigDecimal(systemControlSetting.get("economy_service_charge")),
      business_service_charge = BigDecimal(systemControlSetting.get("business_service_charge")),
      premium_service_charge = BigDecimal(systemControlSetting.get("premium_service_charge")),
      first_service_charge = BigDecimal(systemControlSetting.get("first_service_charge")),
      operation_country = systemControlSetting.get("operation_country")
    ))))
  }

  def save = SecuredAction(roles) { implicit request =>
    form.bindFromRequest().fold(
      error => BadRequest(views.html.admin.systems.controlSetting(error)),
      success => {
        val items = new java.util.HashMap[String, String]()
        items.put("usd2ngn", success.usd2ngn)
        items.put("system_currency", success.system_currency)
        items.put("webpay_api_fee", success.webpay_api_fee)
        items.put("flights_dip", success.flights_dip)
        items.put("hotels_dip", success.hotels_dip)
        items.put("tax_on_commission", success.tax_on_commission.toString)
        items.put("economy_service_charge", success.economy_service_charge.doubleValue().toString)
        items.put("business_service_charge", success.business_service_charge.doubleValue().toString)
        items.put("premium_service_charge", success.premium_service_charge.doubleValue().toString)
        items.put("first_service_charge", success.first_service_charge.doubleValue().toString)
        items.put("operation_country", success.operation_country)
        systemControlSetting.put(items)
        Redirect(routes.ControlSettingsCtrl.index()).flashing(("success", "Settings saved successfully"))
      }
    )
  }

}
