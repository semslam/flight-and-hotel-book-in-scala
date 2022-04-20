package utils.web

import flight.dto.entity.{ ItineraryWSResponse, PNRDetails }
import play.api.mvc.RequestHeader
import mailer.MailService
import models.FlightBookings
import models.Users
import play.twirl.api.Html
import play.api.i18n.Messages
import views.html.web.mails

object Mailer {

  implicit def html2String(html: Html): String = html.toString

  def welcome(user: Users, link: String)(implicit ms: MailService, m: Messages) {
    ms.sendEmailAsync(user.getEmail)(
      subject = Messages("web.mail.welcome.subject"),
      bodyHtml = mails.welcome(user.getFirstName, link),
      bodyText = mails.welcomeTxt(user.getFirstName, link)
    )
  }

  def forgotPassword(email: String, link: String)(implicit ms: MailService, m: Messages) {
    ms.sendEmailAsync(email)(
      subject = Messages("web.mail.forgotpwd.subject"),
      bodyHtml = mails.forgotPassword(email, link),
      bodyText = mails.forgotPasswordTxt(email, link)
    )
  }

}