package controllers.web

import java.util.Objects

import javax.inject.{ Inject, Singleton }
import com.mohiva.play.silhouette.api.Silhouette
import crypto.Encrypt
import models.{ Bookings, Users }
import play.api.i18n.MessagesApi
import utils.silhouette.{ MyEnv, WebController }
import views.html.web.payment._

/**
 * Created by
 * Igbalajobi Jamiu O. on 29/04/2016 9:31 PM.
 */
@Singleton
class PaymentCtrl @Inject() (val silhouette: Silhouette[MyEnv[Users]], val messagesApi: MessagesApi, implicit val encrypt: Encrypt) extends WebController {

  def getTransactionResponse(gatewayName: String) = TODO

  def payment(transactionRef: String) = UserAwareAction { implicit request =>
    val booking = Bookings.find.where().eq("transactionRef", transactionRef.toUpperCase()).findUnique()
    Ok(retryPayment(booking))
  }

}
