package controllers.admin.cms

import java.text.SimpleDateFormat
import java.util.Date

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
import scala.collection.JavaConversions._
import scala.concurrent.Future
import controllers.admin.cms._

/**
 * Created by
 * Igbalajobi Jamiu O. on 01/06/2016 6:44 PM.
 */

case class LinkDAO(
  id: Option[Long] = None,
  name: String,
  hrefURL: Option[String] = None,
  serviceType: String,
  adtUnit: Option[Int] = None,
  chdUnit: Option[Int] = None,
  infUnit: Option[Int] = None,
  segments: List[Segment] = Nil,
  airline: Option[String] = None
)

case class Segment(departureAirport: Option[String], arrivingAirport: Option[String], departureDate: Option[Date], relativeCurrentDate: Option[Boolean])

class LinkCtrl @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi, implicit val encrypt: Encrypt) extends AdminController {

  val roles = WithRoles(Roles.content_manager.name())

  val linkForm = Form(mapping(
    "id" -> optional(longNumber),
    "name" -> nonEmptyText,
    "hrefURL" -> optional(text),
    "serviceType" -> nonEmptyText,
    "adtUnit" -> optional(number),
    "chdUnit" -> optional(number),
    "infUnit" -> optional(number),
    "segments" -> list(mapping(
      "departureAirport" -> optional(nonEmptyText),
      "arrivingAirport" -> optional(nonEmptyText),
      "departureDate" -> optional(date),
      "relativeCurrentDate" -> optional(boolean)
    )(Segment.apply)(Segment.unapply)),
    "airline" -> optional(text)
  )(LinkDAO.apply)(LinkDAO.unapply))

  def index = SecuredAction(roles).async { implicit request =>
    Future.successful(Ok(links.index(CmsLinks.find.all().asScala.toList)))
  }

  def create(id: String) = SecuredAction(roles) { implicit request =>
    if (id != "") {
      val cmsLink = CmsLinks.find.byId(encrypt.decrypt(id).toLong)
      Ok(links.create(cmsLink.getServiceType match {
        case "FLIGHT" => linkForm.fill(LinkDAO(
          Option(cmsLink.getId),
          cmsLink.getName,
          Option(cmsLink.getHref),
          cmsLink.getServiceType,
          Option(cmsLink.getAdtUnit),
          Option(cmsLink.getChdUnit),
          Option(cmsLink.getInfUnit),
          cmsLink.getLinkSegments.map(a => Segment(Option(a.getDepartAirCode), Option(a.getArrivalAirCode), Option(a.getDate), Option(a.getRelativeDate))).toList,
          Option(cmsLink.getAirlineCode)
        ))
        case "URL" => linkForm.fill(LinkDAO(hrefURL = Some(cmsLink.getHref), serviceType = cmsLink.getServiceType, name = cmsLink.getName))
        case _ => linkForm
      }))
    } else Ok(links.create(linkForm))
  }

  def save = SecuredAction(roles) { implicit request =>
    linkForm.bindFromRequest().fold(
      error => BadRequest(links.create(error)),
      success => {
        val link = new CmsLinks
        link.setServiceType(success.serviceType)
        link.setName(success.name)
        success.serviceType match {
          case "FLIGHT" =>
            link.setAirlineCode(success.airline.orNull)
            link.setAdtUnit(success.adtUnit.getOrElse(1).toInt)
            link.setChdUnit(success.chdUnit.getOrElse(0).toInt)
            link.setInfUnit(success.infUnit.getOrElse(0).toInt)
            CmsLinkFlightSegments.find.where().eq("cms_link_id", success.id.orNull).delete()
            if (success.id.isDefined) { link.setId(success.id.get); link.update() } else link.insert()
            success.segments.foreach { segment =>
              val cmsLinkSegments = new CmsLinkFlightSegments
              cmsLinkSegments.setLinkSegments(link)
              cmsLinkSegments.setDepartAirCode(segment.departureAirport.orNull)
              cmsLinkSegments.setArrivalAirCode(segment.arrivingAirport.orNull)
              cmsLinkSegments.setDate(segment.departureDate.orNull)
              if (segment.relativeCurrentDate.isDefined) {
                cmsLinkSegments.setRelativeDate(segment.relativeCurrentDate.get)
              }
              cmsLinkSegments.save()
            }
          case "URL" =>
            link.setHref(success.hrefURL.getOrElse(""))
            if (success.id.isDefined) {
              link.setId(success.id.get)
              link.update()
            } else link.insert()
          case _ =>
        }
      }
    )
    Redirect(routes.LinkCtrl.index()).flashing(("success", "Link Saved Successfully"))
  }

  def delete(id: String) = SecuredAction(roles) { implicit request =>
    CmsLinks.find.byId(encrypt.decrypt(id).toLong).delete()
    Redirect(routes.LinkCtrl.index()).flashing(("success", "Link build deleted successfully"))
  }

}

