package controllers.admin.systems

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import crypto.Encrypt
import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import views.html.admin.systems._
import utils.silhouette.admin.{ AdminController, WithRoles }
import utils.silhouette.MyEnv

import scala.collection.JavaConverters._

/**
 * Created by
 * Igbalajobi Jamiu O. on 13/06/2016 9:37 AM.
 */
class PaymentCtrl @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi, encrypt: Encrypt) extends AdminController {
  val roles = WithRoles(Roles.operation_manager.name())

  val formFields = Form(tuple(
    "id" -> ignored(None: Option[String]),
    "paymentCategory" -> nonEmptyText,
    "name" -> nonEmptyText,
    "accountNo" -> ignored(None: Option[String]),
    "accountName" -> nonEmptyText,
    "logo" -> ignored(None: Option[String]),
    "markup" -> optional(nonEmptyText),
    "capPice" -> optional(nonEmptyText)
  ))

  def index = SecuredAction(roles) { implicit request =>
    val paymentCategories = PaymentCategories.values()
    Ok(payments.index(paymentCategories))
  }

  def manage(category: String) = SecuredAction(roles) { implicit request =>
    val paymentCategory = PaymentCategories.valueOf(category)
    val paymentMethods = PaymentMethods.find.where.eq("payment_category", paymentCategory).findList.asScala.toList
    implicit val encrypt = this.encrypt
    Ok(payments.manage(paymentMethods, paymentCategory))
  }

  def create(category: String) = SecuredAction(roles) { implicit request =>
    val paymentCategory = PaymentCategories.valueOf(category)
    Ok(payments.create(formFields, paymentCategory))
  }

  def save(category: String) = SecuredAction(roles) { implicit request =>
    formFields.bindFromRequest().fold(
      error => Ok(payments.create(error, PaymentCategories.valueOf(category))),
      success => {
        val paymentMethod = new PaymentMethods
        if (success._1.isDefined) paymentMethod.setId(java.lang.Long.parseLong(success._1.get))
        paymentMethod.setPaymentCategory(PaymentCategories.valueOf(success._2))
        paymentMethod.setName(success._3)
        paymentMethod.setAccountNo(if (success._4.isDefined) success._4.get else "")
        paymentMethod.setMarkup(java.lang.Double.parseDouble(success._7.getOrElse("0")))
        paymentMethod.setCapPrice(java.lang.Double.parseDouble(success._8.getOrElse("0")))
        paymentMethod.setAccountName(success._5)
        paymentMethod.setStatus(models.Status.Active)
        paymentMethod.save()
        Redirect(routes.PaymentCtrl.index).withNewSession.flashing(("success", "Payment Record Saved Successfully."))
      }
    )
  }

}
