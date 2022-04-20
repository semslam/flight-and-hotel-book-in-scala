package controllers.common

import java.io.StringReader
import javax.xml.parsers._

import models.{ PaymentHistories, PaymentStatus }
import org.apache.commons.codec.digest.DigestUtils
import org.xml.sax.InputSource
import play.api.libs.json.{ JsString, JsValue, Json }
import play.api.mvc._
import javax.inject._

import play.api.Play

import scala.concurrent.Future
import views.html.paymentapi._
import views.html.mails._
import org.w3c.dom._
import mailer.MailService
import com._
import payapi.interswitch.WebPay

import scala.concurrent.ExecutionContext.Implicits.global

class PaymentCtrl @Inject() (webPayQuery: WebPay, implicit val mailService: MailService) extends Controller {

  def confirm(paymentCode: String, transRef: String) = Action.async { implicit request =>
    val (transRef: String, amount: String) = paymentCode.toLowerCase() match {
      case "gt" => (request.body.asFormUrlEncoded.get("gtpay_tranx_id").head, request.body.asFormUrlEncoded.get("gtpay_tranx_amt").head) //GTPay
      case "gp" => (request.getQueryString("txnref"), "")
      case "gt" => (request.getQueryString("txnref"), "")
    }
    webPayQuery.queryTransaction(paymentCode = paymentCode, transRef = transRef, amount = amount).map { WebPaymentQuery =>
      val paymentHistory = PaymentHistories.find.where().eq("transRef", transRef).findUnique()
      if (WebPaymentQuery.isSuccessful) {
        paymentHistory.setStatus(PaymentStatus.Paid)
        paymentHistory.setAmountPaid(WebPaymentQuery.amount.toDouble) //Amount return from Gateway
        paymentHistory.setGatewayResponseAmt(WebPaymentQuery.amount)
      } else {
        paymentHistory.setStatus(PaymentStatus.Failed)
      }
      paymentHistory.setGatewayResponseCode(WebPaymentQuery.transCode)
      paymentHistory.setGatewayResponseDesc(WebPaymentQuery.transDesc)
      paymentHistory.setGatewayResponseCurrency(WebPaymentQuery.currency)
      paymentHistory.setEftApiResponse(WebPaymentQuery.toJson.getOrElse(""))
      paymentHistory.setId(paymentHistory.getId)
      paymentHistory.update()
      //Send a mail to the customer confirming payment
      import scala.collection.JavaConverters._
      val msg = webPaymentConfirmation(paymentHistory).body
      //TODO
      //      mailService.sendEmailAsync(paymentHistory.getFlightBookingsList.asScala.toList.head.getContactEmail)("Payment Receipt", msg, msg)
      Ok(onlinePayment(WebPaymentQuery))
    }
  }

}