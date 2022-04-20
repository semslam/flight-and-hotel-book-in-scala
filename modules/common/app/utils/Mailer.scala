package utils

import flight.dto.entity.{ ItineraryWSResponse, PNRDetails }
import hotel.dto.entity.booking.BookingHotelResponse
import mailer.MailService
import play.api.mvc.RequestHeader
import models._
import play.twirl.api.Html
import play.api.i18n.Messages
import views.html.mails
import utils._
import scala.concurrent.{ ExecutionContext, Future }

object Mailer {

  implicit def html2String(html: Html): String = html.toString

  def cancelBooking(flightBookings: FlightBookings)(implicit ms: MailService) = {
    ms.sendEmailAsync(flightBookings.getBookings.getContactEmail)(
      subject = s"${flightBookings.getBookings.getTransactionRef} Booking Cancelled",
      bodyHtml = mails.cancelledBooking(flightBookings),
      bodyText = mails.cancelledBooking(flightBookings)
    )
  }

  def paymentReminder(flightBookings: FlightBookings)(implicit ms: MailService) = {
    ms.sendEmailAsync(flightBookings.getBookings.getContactEmail)(
      subject = s"${flightBookings.getBookings.getTransactionRef} Payment Reminder",
      bodyHtml = mails.paymentReminder(flightBookings),
      bodyText = mails.paymentReminder(flightBookings)
    )
  }

  def sendMail(email: String, html: Html)(implicit ms: MailService) = ms.sendEmailAsync(email)(
    subject = "Testing",
    bodyHtml = html,
    bodyText = html.toString()
  )

  def flightBooking(pnrDetail: PNRDetails, booking: Bookings, itinerary: ItineraryWSResponse)(implicit ms: MailService, r: RequestHeader) = {
    val flightBookings = FlightBookings.find.where().eq("bookings", booking).findUnique()
    val template = if (flightBookings.getBookings.getSalesCategory.equals(SalesCategory.B2B)) {
      mails.b2b.flightInvoice(flightBookings, itinerary)
    } else {
      mails.b2c.flightInvoice(pnrDetail, flightBookings, itinerary)
    }
    ms.sendEmailAsync(flightBookings.getBookings.getContactEmail)(
      subject = s"${play.Configuration.root().getString("project.name")} Booking Confirmation",
      bodyHtml = template,
      bodyText = template.toString
    )
  }

  def failedBooking(booking: Bookings, itinerary: ItineraryWSResponse)(implicit ms: MailService, r: RequestHeader) = {
    val flightBookings = FlightBookings.find.where().eq("bookings", booking).findUnique()
    val template = if (flightBookings.getBookings.getSalesCategory.eq(SalesCategory.B2B)) {
      mails.b2b.failedBooking(flightBookings, itinerary)
    } else {
      mails.b2c.failedBooking(flightBookings, itinerary)
    }
    ms.sendEmailAsync(flightBookings.getBookings.getContactEmail, play.Configuration.root().getString("project.email"))(
      subject = s"${flightBookings.getBookings.getTransactionRef} Booking Failed",
      bodyHtml = template,
      bodyText = template.toString()
    )
  }

  def hotelBooking(booking: Bookings, hotelResponse: BookingHotelResponse)(implicit ex: ExecutionContext, ms: MailService, request: RequestHeader) = Future {
    booking.getStatus match {
      case BookingStatus.FAILED | BookingStatus.FAILED_BOOKING =>
      case _ =>
        val htmlBody = mails.b2c.hotelBookingSuccess(booking, hotelResponse)
        ms.sendEmailAsync(booking.getHotelBookings.getBookings.getContactEmail)(
          subject = "Hotel Booking Confirmation",
          bodyHtml = htmlBody,
          bodyText = htmlBody.toString
        )
    }
  }
}