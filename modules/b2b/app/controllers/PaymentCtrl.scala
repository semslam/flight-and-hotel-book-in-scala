package controllers.b2b

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import crypto.Encrypt
import models._
import play.api._
import play.api.i18n.{ Lang, MessagesApi }
import play.api.mvc._
import utils.silhouette._

import scala.concurrent.Future
import views.html.b2b._
import utils.silhouette.b2b._

class PaymentCtrl @Inject() (val silhouette: Silhouette[MyEnv[Users]], val messagesApi: MessagesApi, encrypt: Encrypt) extends B2BController {

  implicit val enc = encrypt

  def flCardPayment(paymentId: String, fullName: String, email: String) = SecuredAction { implicit request =>
    val paymentItem = PaymentHistories.find.where.eq("id", java.lang.Long.parseLong(enc.decrypt(paymentId))).findUnique()
    Ok(payment.flCardPayment(paymentItem, fullName, email))
  }

}