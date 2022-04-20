package controllers.admin.cms

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import crypto.Encrypt
import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.data._
import play.api.i18n.MessagesApi
import utils.silhouette.MyEnv
import views.html.admin.cms._

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.Future
import controllers.admin.cms._
import models.admin.MailTokenManager
import utils.silhouette.admin.{ AdminController, ManagerService, WithRole, WithRoles }

/**
 * Created by
 * Igbalajobi Jamiu O. on 28/05/2016 1:52 PM.
 */

case class Fragment(id: Option[String], categoryId: Option[String], name: String, htmlCode: String, isPublish: String, headTitle: Option[String])

class FragmentCtrl @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi, encrypt: Encrypt) extends AdminController {

  val roles = WithRoles(Roles.content_manager.name())
  implicit val enc = this.encrypt

  val containerForm = Form(mapping(
    "id" -> optional(nonEmptyText),
    "categoryId" -> optional(nonEmptyText),
    "name" -> nonEmptyText.verifying(minLength(2)),
    "htmlCode" -> nonEmptyText,
    "isPublish" -> nonEmptyText,
    "headTitle" -> optional(nonEmptyText)
  )(Fragment.apply)(Fragment.unapply))

  def index = SecuredAction(roles).async { implicit request =>
    Future.successful {
      Ok(fragments.index(CmsFragments.find.all().asScala.toList))
    }
  }

  def create(id: String) = SecuredAction(roles).async { implicit request =>
    Future.successful {
      if (id != null && !id.isEmpty) {
        val dec = encrypt.decrypt(id)
        val data = CmsFragments.find.byId(java.lang.Long.valueOf(dec))
        val categoryId = if (data.getCategoryId == null) {
          None
        } else {
          Some(data.getCategoryId.getId.toString)
        }
        Ok(fragments.create(containerForm.fill(Fragment(Some(data.getId.toString), categoryId, data.getName, data.getHtmlCode, data.getIsPublish.name(), Option(data.getHeadTitle)))))
      } else Ok(fragments.create(containerForm))
    }
  }

  def save = SecuredAction(roles) { implicit request =>
    containerForm.bindFromRequest.fold(
      error => BadRequest(fragments.create(error)),
      form => {
        val container = new CmsFragments
        container.setHeadTitle(form.headTitle.orNull)
        container.setHtmlCode(form.htmlCode)
        form.categoryId match {
          case Some(_) => container.setCategoryId(CmsContentCategories.find.byId(java.lang.Long.parseLong(form.categoryId.get)))
          case _ => container.setCategoryId(null)
        }
        container.setName(form.name)
        container.setIsPublish(YesNoEnum.valueOf(form.isPublish))
        container.setAuthUserId(request.identity)
        if (form.id.isDefined) {
          container.setId(enc.decrypt(form.id.get).toLong)
          container.update()
        } else container.insert()
        Redirect(routes.FragmentCtrl.index()).withNewSession.flashing(("success", "Container saved successfully"))
      }
    )
  }

  def delete(id: String) = SecuredAction(roles).async {
    Future.successful {
      CmsFragments.find.byId(java.lang.Long.parseLong(encrypt.decrypt(id))).delete()
      Redirect(routes.FragmentCtrl.index()).withNewSession.flashing(("success", "Container deleted successfully"))
    }
  }

  def toJson = SecuredAction(roles).async {
    Future.successful(Ok(""))
  }

}

object FragmentWidgets {
  val widgets = scala.collection.immutable.HashMap[String, String](
    "[flightBookingEngine]" -> "Flight Booking Engine",
    "[hotelBookingEngine]" -> "Hotels Booking Engine",
    "[deal code=[input]]" -> "Deals",
    "[fragment code=[input]]" -> "Fragment",
    "[flightPackages]" -> "Flight Packages Only",
    "[hotelPackages]" -> "Hotel Packages Only",
    "[flightHotelPackages]" -> "Flight + Hotel Packages Only",
    "[logo]" -> "Display Logo",
    "[menu]" -> "Menu",
    "[currency]" -> "Currency",
    "[shoppingCart]" -> "Shopping Cart",
    "[csrfToken]" -> "Cross-site/Domain Token(CSRF)",
    s"""[banner code=UNIQ_CODE]""" -> "Insert Banner to Page",
    s"""[package code=UNIQ_CODE]""" -> "Insert Package to Page",
    s"""[deal code=UNIQ_CODE]""" -> "Insert Deal to Page"
  )
}
