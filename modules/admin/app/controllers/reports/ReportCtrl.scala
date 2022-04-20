package controllers.admin.reports

import java.io._
import java.text.SimpleDateFormat
import java.util
import java.util.Date
import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import models.Users.Genders
import models._
import org.apache.commons.csv.{ CSVFormat, CSVPrinter }
import utils.silhouette.MyEnv
//import org.apache.commons.io.IOUtils
import play.api.libs.iteratee.Enumerator

//import org.jxls.common.Context
//import org.jxls.util.JxlsHelper
import play.api.Play
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.data.Form
import utils.silhouette.admin.{ AdminController, WithRoles }
import utils.silhouette.MyEnv
import play.api.data.Forms._
import play.api.Play.current
import scala.collection.JavaConverters._

/**
 * Created by Igbalajobi Jamiu O. on 05/01/2017 2:01 PM.
 */
class ReportCtrl @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi) extends AdminController {
  val dtFormat = new SimpleDateFormat("yyyy-MM-dd")
  val roles = WithRoles(Roles.finance.name(), Roles.operation_manager.name(), Roles.admin.name())
  val reportForm = Form(mapping(
    "reportType" -> nonEmptyText,
    "fromDate" -> optional(nonEmptyText),
    "toDate" -> optional(nonEmptyText),
    "salesCategory" -> optional(nonEmptyText),
    "paymentStatus" -> optional(nonEmptyText),
    "ticketStatus" -> optional(nonEmptyText),
    "queueType" -> optional(nonEmptyText),
    "ticketingPartner" -> optional(nonEmptyText),
    "destinationLocale" -> optional(nonEmptyText),
    "usersId" -> optional(list(nonEmptyText))
  )(QueryForm.apply)(QueryForm.unapply))

  val userNewsletterForm = Form(mapping(
    "reportType" -> nonEmptyText,
    "fromDate" -> optional(nonEmptyText),
    "toDate" -> optional(nonEmptyText),
    "role" -> optional(nonEmptyText)
  )(UserReportForm.apply)(UserReportForm.unapply))

  def generateReport() = SecuredAction(roles) { implicit request =>
    Ok(views.html.admin.reports.generateReport(reportForm))
  }

  def postGenerateReport = SecuredAction(roles) { implicit request =>
    reportForm.bindFromRequest.fold(
      hasError => BadRequest(views.html.admin.reports.generateReport(hasError)).flashing(("error", "Request failed.")),
      success => {
        success.reportType match {
          case "fl_booking" =>
            val query = FlightBookings.find.where()
            if (success.salesCategory.nonEmpty) {
              query.where().eq("bookings.salesCategory", success.salesCategory.get)
            }
            if (success.paymentStatus.nonEmpty) {
              query.where().eq("bookings.paymentHistoryId.status", PaymentStatus.valueOf(success.paymentStatus.get))
            }
            if (success.ticketStatus.nonEmpty) {
              query.where().eq("bookings.status", BookingStatus.valueOf(success.ticketStatus.get))
            }
            if (success.fromDate.nonEmpty) {
              val toDate = success.toDate.getOrElse(dtFormat.format(new Date()))
              val fromDate = dtFormat.parse(success.fromDate.get)
              query.between("t0.created_at", dtFormat.format(fromDate), toDate)
            }
            if (success.ticketingPartner.nonEmpty) {
              query.where().eq("ticketing_partner_id", success.ticketingPartner.get)
            }
            if (success.destinationLocale.nonEmpty) {
              query.where().eq("destination_locale", success.destinationLocale.get)
            }
            if (success.usersId.nonEmpty) {
              import scala.collection.JavaConverters._
              query.where().in("t0.user_id", success.usersId.get.asJava)
            }

            val flightBookings = query.findList()
            try {
              /*
              * XLS File Write.
              * Bug - Not working.
              *               val pathSeparator = File.separator
              *              val objectOutput = new FileOutputStream(s"${play.Configuration.root().getString("")}${path}$fileName")
              * val playPath = s"${Play.application.path.getPath}${pathSeparator}modules${pathSeparator}common${pathSeparator}conf${pathSeparator}xlsTemplates$pathSeparator"
              * play.Logger.info("Path: " + playPath + " : Sep: " + pathSeparator)
              * val is: InputStream = IOUtils.toInputStream(s"${playPath}flight-report-template.xls")
              */

              /*
               * CSV Format
               */
              val csvHeader = Array("Date", "TD Reference", "Vendor", "GDS Ref.", "Source", "Sales Category", "Supplier", "Trip Type", "Departure", "Arrival", "Region", "Contact Name", "Contact Email", "Contact Phone", "Currency", "NUC", "GDS Tax", "GDS Total Fare", "MU/Commission(%)", "MU/Commission Equiv", "Amount Paid", "Ticket Status", "Payment Status", "Payment Method", "Ticketing Partner")
              val fileName = s"${play.Configuration.root().getString("rsc.folder")}${dtFormat.format(new Date())}-${success.reportType}.csv"
              val file = csvExporter(csvHeader = csvHeader, fileName = fileName, (csvFilePrinter: CSVPrinter) => flightBookings.asScala.toList.foreach { item =>
                val items: java.util.List[AnyRef] = new util.ArrayList[AnyRef]()
                items.add(dtFormat.format(item.getCreatedAt))
                items.add(item.getBookings.getTransactionRef)
                items.add(item.getBookings.getSupplier)
                items.add(item.getPnrRef)
                //                items.add(item.source.name())
                items.add(item.getBookings.getSalesCategory.name())
                items.add(item.getAirlineCode)
                items.add(item.getTripType.name())
                items.add(item.getOriginDestinationsList.get(0).getDepartureAirportCode)
                items.add(item.getOriginDestinationsList.get(0).getArrivalAirportCode)
                items.add(item.getDestinationLocale)
                items.add(s"${item.getBookings.getContactTitle} ${item.getBookings.getContactFirstname}  ${item.getBookings.getContactSurname} ")
                items.add(s"${item.getBookings.getContactEmail}")
                items.add(s"${item.getBookings.getContactPhone}")
                items.add(item.getBookings.getPaymentHistoryId.getCurrency)
                items.add(item.getGdsBaseFare.toString)
                items.add(item.getGdsTaxFare.toString)
                items.add(item.getGdsTotalFare.toString)
                items.add(s"${Option(item.getBookings.getPaymentHistoryId.getCommissionDispenseValue).orNull} ${Option(item.getBookings.getPaymentHistoryId.getCommissionValueType).orNull}")
                items.add(item.getBookings.getPaymentHistoryId.getCommissionDispenseValueAmount.toString)
                items.add(item.getBookings.getPaymentHistoryId.getTotalAmount.toString)
                items.add(item.getBookings.getStatus.name())
                items.add(item.getBookings.getPaymentHistoryId.getStatus.name())
                items.add("") //item.paymentHistoryId.paymentMethodId.name
                items.add(Option(item.getTicketingPartner) match {
                  case Some(_) => item.getTicketingPartner.getName
                  case _ => "N/A"
                })
                csvFilePrinter.printRecord(items)
              })
              Ok.sendFile(file)
            } catch {
              case e: Exception => e.printStackTrace(); Redirect(routes.ReportCtrl.generateReport()).flashing(("error", "Request failed"))
            }
          case "ht_booking" => Redirect(routes.ReportCtrl.generateReport()).flashing(("error", "Request failed"))
        }
      }
    )
  }

  def usersReport() = SecuredAction(roles) { implicit request =>
    Ok(views.html.admin.reports.usersNewsletter(userNewsletterForm))
  }

  def usersReportPost() = SecuredAction(roles) { implicit request =>
    userNewsletterForm.bindFromRequest().fold(
      error => BadRequest(views.html.admin.reports.usersNewsletter(error)),
      value => {
        val fileName = s"${play.Configuration.root().getString("rsc.folder")}${new Date().getTime}-${value.reportType}.csv"
        value.reportType match {
          case "users_r" =>
            val query = Users.find.where()
            if (value.fromDate.nonEmpty) {
              val toDate = value.toDate.getOrElse(dtFormat.format(new Date()))
              val fromDate = dtFormat.parse(value.fromDate.get)
              query.between("created_at", dtFormat.format(fromDate), toDate)
            }
            if (value.role.nonEmpty) {
              value.role.get match {
                case "B2B" => query.where().in("role", Roles.b2b_owner.name(), Roles.b2b_sales_agent.name())
                case "B2C" => query.where().eq("role", Roles.b2c_customer.name())
              }
            }
            val csvHeader = Array("ID", "Email", "Phone", "Full Name", "Role", "Gender")
            Ok.sendFile(csvExporter(csvHeader = csvHeader, fileName = fileName, (csvPrinter: CSVPrinter) => query.findList().asScala.toList.foreach { item =>
              val items: java.util.List[AnyRef] = new util.ArrayList[AnyRef]()
              items.add(item.getId.toString)
              items.add(item.getEmail)
              items.add(item.getPhone)
              items.add(item.fullName())
              items.add(item.getRole)
              items.add(Option(item.getGender).getOrElse(Genders.MALE).name())
              csvPrinter.printRecord(items)
            }))
          case "newsletter_r" =>
            val csvHeader = Array("ID", "Email")
            val query = NewsletterUsers.find.where()
            if (value.fromDate.nonEmpty) {
              val toDate = value.toDate.getOrElse(dtFormat.format(new Date()))
              val fromDate = dtFormat.parse(value.fromDate.get)
              query.between("created_at", dtFormat.format(fromDate), toDate)
            }
            Ok.sendFile(csvExporter(csvHeader = csvHeader, fileName = fileName, (csvPrinter: CSVPrinter) => query.findList().asScala.toList.foreach { item =>
              val items: java.util.List[AnyRef] = new util.ArrayList[AnyRef]()
              items.add(item.getId.toString)
              items.add(item.getEmail)
              csvPrinter.printRecord(items)
            }))
        }
      }
    )
  }

  def csvExporter(csvHeader: Array[String], fileName: String, item: (CSVPrinter) => (AnyVal)) = {
    val csvFilePrinter = new CSVPrinter(new FileWriter(fileName), CSVFormat.DEFAULT.withRecordSeparator("\n"))
    csvFilePrinter.printRecord(csvHeader.toSeq.asJava)
    item(csvFilePrinter)
    csvFilePrinter.close()
    new File(fileName)
  }

}

case class QueryForm(reportType: String, fromDate: Option[String], toDate: Option[String], salesCategory: Option[String], paymentStatus: Option[String], ticketStatus: Option[String], queueType: Option[String], ticketingPartner: Option[String], destinationLocale: Option[String], usersId: Option[List[String]])

case class UserReportForm(reportType: String, fromDate: Option[String], toDate: Option[String], role: Option[String])
