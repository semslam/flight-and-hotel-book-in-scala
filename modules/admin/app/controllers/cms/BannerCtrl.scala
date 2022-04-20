package controllers.admin.cms

import java.text.SimpleDateFormat
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
import scala.concurrent.Future
import controllers.admin.cms._

/**
 * Created by
 * Igbalajobi Jamiu O. on 29/05/2016 8:45 AM.
 */
case class BannerDOA(
  id: Option[Long],
  name: String,
  code: String,
  html: String,
  htmlAlt: Option[String],
  cmsLinkId: Option[Long]
)

class BannerCtrl @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi, encrypt: Encrypt) extends AdminController {

  val roles = WithRoles(Roles.content_manager.name())
  implicit val enc = this.encrypt

  val bannerForm = Form(mapping(
    "id" -> optional(longNumber),
    "name" -> nonEmptyText,
    "code" -> nonEmptyText.verifying("Unique Code Exceeded 6 Characters", code => code.length <= 6), //verifying("Unique Code Already Exists", code => CmsBanners.find.where.eq("code", code).findRowCount() <= 0)
    "html" -> nonEmptyText,
    "htmlAlt" -> optional(nonEmptyText),
    "cmsLinkId" -> optional(longNumber)
  )(BannerDOA.apply)(BannerDOA.unapply))

  def index = SecuredAction(roles) { implicit request =>
    Ok(banner.index(CmsBanners.find.all().asScala.toList))
  }

  def create(id: String) = SecuredAction(roles) { implicit request =>
    if (id != null && !id.isEmpty) {
      val dec = encrypt.decrypt(id)
      val data = CmsBanners.find.byId(dec.toLong)
      val bannerDOA = BannerDOA(Some(data.getId), data.getName, data.getCode, data.getHtmlCode, Option(data.getHtmlCodeAlternate), if (data.getCmsLink != null) Some(data.getCmsLink.getId) else None)
      Ok(banner.create(bannerForm.fill(bannerDOA)))
    } else Ok(banner.create(bannerForm))
  }

  def save = SecuredAction(roles) { implicit request =>
    bannerForm.bindFromRequest().fold(
      error => {
        BadRequest(banner.create(error))
      },
      input => {
        val cmsBaner = new CmsBanners
        cmsBaner.setName(input.name)
        cmsBaner.setAuthUserId(request.identity)
        cmsBaner.setHtmlCode(input.html)
        cmsBaner.setHtmlCodeAlternate(input.htmlAlt.getOrElse(input.html))
        if (input.cmsLinkId.isDefined) {
          cmsBaner.setCmsLink(CmsLinks.find.byId(input.cmsLinkId.get))
        }
        if (input.id.isDefined) {
          cmsBaner.setId(input.id.get); cmsBaner.update()
        } else {
          cmsBaner.setCode(input.code); cmsBaner.insert()
        }
        Redirect(routes.BannerCtrl.index).withNewSession.flashing(("success", "Banner Saved Successfully"))
      }
    )
  }

  def delete(id: String) = SecuredAction(roles).async { implicit request =>
    Future.successful {
      CmsBanners.find.byId(encrypt.decrypt(id).toLong).delete()
      Redirect(routes.BannerCtrl.index).withNewSession.flashing(("success", "Banner deleted successfully."))
    }
  }

}
