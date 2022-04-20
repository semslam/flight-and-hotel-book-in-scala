package controllers.admin.cms

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
case class PageDOA(
  id: Option[String],
  name: String,
  cmsTemplateId: String,
  metaKeywords: Option[String],
  metaDesc: Option[String],
  isPublish: String,
  host: List[String],
  slugUrl: String,
  inlineCss: Option[String],
  inlineJs: Option[String]
)

class PageCtrl @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi, encrypt: Encrypt) extends AdminController {

  val roles = WithRoles(Roles.content_manager.name())
  implicit val enc = this.encrypt

  val pageForm = Form(mapping(
    "id" -> optional(nonEmptyText),
    "name" -> nonEmptyText,
    "cmsTemplateId" -> nonEmptyText,
    "metaKeywords" -> optional(nonEmptyText),
    "metaDesc" -> optional(nonEmptyText),
    "isPublish" -> nonEmptyText,
    "host" -> list(nonEmptyText),
    "slugUrl" -> nonEmptyText,
    "inlineCss" -> optional(nonEmptyText),
    "inlineJs" -> optional(nonEmptyText)
  )(PageDOA.apply)(PageDOA.unapply))

  def index = SecuredAction(roles) { implicit request =>
    Ok(pages.index(CmsPages.find.all().asScala.toList))
  }

  def create(id: String) = SecuredAction(roles) { implicit request =>
    if (id != null && !id.isEmpty) {
      val dec = encrypt.decrypt(id)
      val data = CmsPages.find.byId(dec.toLong)
      Ok(pages.create(pageForm.fill(PageDOA(Some(data.getId.toString), data.getName, data.getCmsTemplateId.getId.toString, Some(data.getMetaKeywords), Some(data.getMetaDescription),
        data.getIsPublish.name(), Nil, data.getSlugUrl, Some(data.getInlineCss), Some(data.getInlineJs)))))
    } else Ok(pages.create(pageForm))
  }

  def save = SecuredAction(roles) { implicit request =>
    pageForm.bindFromRequest().fold(
      error => BadRequest(pages.create(error)),
      input => {
        val cmsPage = new CmsPages
        cmsPage.setName(input.name)
        cmsPage.setCmsTemplateId(CmsTemplates.find.byId(input.cmsTemplateId.toLong))
        cmsPage.setMetaKeywords(input.metaKeywords.orNull)
        cmsPage.setMetaDescription(input.metaDesc.orNull)
        cmsPage.setIsPublish(YesNoEnum.valueOf(input.isPublish))
        cmsPage.setSlugUrl(input.slugUrl)
        //        cmsPage.setHost(input.host.map(host => s"$host<<>>").head)
        input.id match {
          case s: Some[String] =>
            cmsPage.setId(s.get.toLong); cmsPage.update()
          case _ => cmsPage.insert()
        }
        Redirect(routes.PageCtrl.index).withNewSession.flashing(("success", "Page Saved Successfully"))
      }
    )
  }

  def delete(id: String) = SecuredAction(roles).async { implicit request =>
    Future.successful {
      val page = CmsPages.find.byId(encrypt.decrypt(id).toLong)
      page.setSlugUrl(null)
      page.update()
      page.delete()
      Redirect(routes.PageCtrl.index).withNewSession.flashing(("success", "Page deleted successfully."))
    }
  }

}
