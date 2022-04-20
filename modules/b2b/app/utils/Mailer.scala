package utils.b2b

import flight.dto.entity.{ ItineraryWSResponse, PNRDetails }
import models.{ BookingStatus, FlightBookings, SalesCategory, Users }
import play.api.mvc.RequestHeader
import mailer.MailService
import play.twirl.api.Html
import play.api.i18n.Messages
import views.html.b2b.mails

object Mailer {

  implicit def html2String(html: Html): String = html.toString

  def activateAccount(email: String, link: String)(implicit ms: MailService, m: Messages) {
    ms.sendEmailAsync(email)(
      subject = Messages("b2b.mail.forgotpwd.subject"),
      bodyHtml = mails.forgotPassword(email, link),
      bodyText = mails.forgotPasswordTxt(email, link)
    )
  }

  def agentSignUp(user: Users, link: String)(implicit ms: MailService, m: Messages) {
    ms.sendEmailAsync(user.getEmail)(
      subject = Messages("b2b.mail.account.review", play.Configuration.root().getString("project.name")),
      bodyHtml = mails.welcome(user.getFirstName, link),
      bodyText = mails.welcomeTxt(user.getFirstName, link)
    )
  }

  def forgotPassword(email: String, link: String)(implicit ms: MailService, m: Messages) {
    ms.sendEmailAsync(email)(
      subject = Messages("admin.mail.forgotpwd.subject"),
      bodyHtml = mails.forgotPassword(email, link),
      bodyText = mails.forgotPasswordTxt(email, link)
    )
  }

  def flightBooking(flightBookings: FlightBookings, itinerary: ItineraryWSResponse, email: String = "")(implicit ms: MailService, m: Messages, r: RequestHeader) = {
    flightBookings.getBookings.getStatus match {
      case BookingStatus.CANCELLED => utils.Mailer.cancelBooking(flightBookings)
      case BookingStatus.TICKET_ISSUED => ms.sendEmailAsync(flightBookings.getBookings.getContactEmail)(s"${flightBookings.getBookings.getUserId.getAgentCorporateDetailId.getCompanyName} eTicket ${flightBookings.getBookings.getTransactionRef}", views.html.b2b.mails.flightTicket(flightBookings).body, views.html.b2b.mails.flightTicket(flightBookings).body)
      case _ => ms.sendEmailAsync(flightBookings.getBookings.getContactEmail)(s"${flightBookings.getBookings.getUserId.getAgentCorporateDetailId.getCompanyName} Booking Confirmation", views.html.b2b.mails.flightInvoice(flightBookings).body, views.html.b2b.mails.flightInvoice(flightBookings).body)
    }
  }

}