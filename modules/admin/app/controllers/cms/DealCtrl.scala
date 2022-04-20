package controllers.admin.cms

import java.text.SimpleDateFormat
import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import crypto.Encrypt
import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.MessagesApi
import utils.silhouette.MyEnv
import views.html.admin.cms._
import models.admin.MailTokenManager
import utils.silhouette.admin.{ AdminController, ManagerService, WithRole, WithRoles }

import scala.collection.JavaConverters._
import scala.concurrent.Future
import controllers.admin.cms._

/**
 * Created by
 * Igbalajobi Jamiu O. on 29/05/2016 8:45 AM.
 */
case class DealDOA(
  id: Option[String],
  name: String,
  code: String,
  serviceType: String,
  carrierName: String,
  originCity: String,
  destinationCity: String,
  url: Option[String],
  imageUrl: String,
  expireOn: Option[String],
  priority: String,
  priceFrom: String
)

class DealCtrl @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi, encrypt: Encrypt) extends AdminController {

  val roles = WithRoles(Roles.content_manager.name())
  implicit val enc = this.encrypt

  val dealForm = Form(mapping(
    "id" -> optional(nonEmptyText),
    "name" -> nonEmptyText,
    "code" -> nonEmptyText.verifying("Unique Code Already Exists", code => Deals.find.where.eq("code", code).findRowCount() <= 0).verifying("Unique Code Exceeded 6 Characters", code => code.length <= 6),
    "serviceType" -> nonEmptyText,
    "carrierName" -> nonEmptyText,
    "originCity" -> nonEmptyText,
    "destinationCity" -> nonEmptyText,
    "url" -> optional(nonEmptyText),
    "imageUrl" -> nonEmptyText,
    "expireOn" -> optional(nonEmptyText),
    "priority" -> nonEmptyText,
    "priceFrom" -> nonEmptyText
  )(DealDOA.apply)(DealDOA.unapply))

  def index = SecuredAction(roles) { implicit request =>
    Ok(deal.index(Deals.find.all().asScala.toList))
  }

  def create(id: String) = SecuredAction(roles) { implicit request =>
    if (id != null && !id.isEmpty) {
      val dec = encrypt.decrypt(id)
      val data = Deals.find.byId(dec.toLong)
      val dealDOA = DealDOA(Some(data.getId.toString), data.getName, data.getCode, data.getServiceType.name(), data.getCarrierName, data.getOriginCity, data.getDestinationCity, Option(data.getUrl),
        data.getImageUrl, Some(""), data.getPriority, data.getPriceFrom)
      Ok(deal.create(dealForm.fill(dealDOA)))
    } else Ok(deal.create(dealForm))
  }

  def save = SecuredAction(roles) { implicit request =>
    dealForm.bindFromRequest().fold(
      error => BadRequest(deal.create(error)),
      input => {
        val deals = new Deals
        deals.setName(input.name)
        deals.setCode(input.code)
        deals.setAuthUserId(request.identity)
        deals.setCarrierName(input.carrierName)
        deals.setDestinationCity(input.destinationCity)
        deals.setOriginCity(input.originCity)
        deals.setPriceFrom(input.priceFrom)
        deals.setServiceType(Services.valueOf(input.serviceType))
        deals.setUrl(input.url.orNull)
        deals.setImageUrl(input.imageUrl)
        input.id match {
          case s: Some[String] =>
            deals.setId(s.get.toLong); deals.update()
          case _ => deals.insert()
        }
        Redirect(routes.DealCtrl.index).withNewSession.flashing(("success", "Deal Saved Successfully"))
      }
    )
  }

  def delete(id: String) = SecuredAction(roles).async { implicit request =>
    Future.successful {
      Deals.find.byId(encrypt.decrypt(id).toLong).delete()
      Redirect(routes.DealCtrl.index).withNewSession.flashing(("success", "Deal deleted successfully."))
    }
  }

}
