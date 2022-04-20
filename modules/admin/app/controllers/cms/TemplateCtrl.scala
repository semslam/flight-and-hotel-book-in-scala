package controllers.admin.cms

import javax.inject.Inject

import cms.DynamicPageHandler
import com.mohiva.play.silhouette.api.Silhouette
import crypto.Encrypt
import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.i18n.MessagesApi
import play.api.libs.json.{ JsObject, JsString }
import utils.silhouette.MyEnv

import models.admin.MailTokenManager
import utils.silhouette.admin.{ AdminController, ManagerService, WithRole, WithRoles }
import scala.concurrent._
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import views.html.admin.cms._
import controllers.admin.cms._

/**
 * Created by
 * Igbalajobi Jamiu O. on 29/05/2016 8:17 AM.
 */
case class TemplateDOA(id: Option[String], name: String, categoryId: Option[String], metaKeywords: Option[String], metaDesc: Option[String], slugUrl: String, inlineCss: Option[String], inlineJs: Option[String], action: String)

class TemplateCtrl @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi, encrypt: Encrypt) extends AdminController {

  val roles = WithRoles(Roles.content_manager.name())
  implicit val enc = this.encrypt

  val form = Form(mapping(
    "id" -> optional(nonEmptyText),
    "name" -> nonEmptyText,
    "categoryId" -> optional(nonEmptyText),
    "metaKeywords" -> optional(nonEmptyText),
    "metaDesc" -> optional(nonEmptyText),
    "slugUrl" -> nonEmptyText,
    "inlineCss" -> optional(nonEmptyText),
    "inlineJs" -> optional(nonEmptyText),
    "action" -> nonEmptyText
  )(TemplateDOA.apply)(TemplateDOA.unapply))

  def index = SecuredAction(roles).async { implicit request =>
    Future.successful(Ok(templates.index(CmsTemplates.find.all().asScala.toList)))
  }

  def items(id: String) = SecuredAction(roles).async {
    Future.successful(Ok(""))
  }

  def previewTemplate(id: String) = SecuredAction(roles).async { implicit request =>
    Future.successful {
      val cmsTemplates = CmsTemplates.find.byId(enc.decrypt(id).toLong)
      val page = new CmsPages
      val templatesManager = new DynamicPageHandler().parsePage(page)
      page.setCmsTemplateId(cmsTemplates)
      page.setName(cmsTemplates.getName)
      Ok(templates.preview(page, templatesManager))
    }
  }

  def create() = SecuredAction(roles) { implicit request =>
    Ok(templates.create(form))
  }

  def edit(id: String, pageId: String) = SecuredAction(roles) { implicit request =>
    val data = CmsTemplates.find.byId(java.lang.Long.valueOf(encrypt.decrypt(id)))
    val page = CmsPages.find.byId(java.lang.Long.parseLong(encrypt.decrypt(pageId)))
    val templateFill = TemplateDOA(
      id = Some(data.getId.toString),
      name = data.getName,
      categoryId = Option(if (data.getCategoryId != null) data.getCategoryId.getId.toString else null),
      metaKeywords = Some(data.getCmsTemplateAttrList.find(_.getAttr == "metaKeywords").getOrElse(new CmsTemplateAttr).getValue),
      metaDesc = Some(data.getCmsTemplateAttrList.find(_.getAttr == "metaKeywords").getOrElse(new CmsTemplateAttr).getValue),
      slugUrl = page.getSlugUrl,
      inlineCss = Option(page.getInlineCss),
      inlineJs = Option(page.getInlineJs),
      action = "save_publish"
    )
    //
    Ok(templates.edit(form.fill(templateFill), data, page))
  }

  def save = SecuredAction(roles) { implicit request =>
    form.bindFromRequest().fold(
      formError => BadRequest(templates.create(formError)),
      formData => {
        val cmsTemplate = new CmsTemplates
        cmsTemplate.setName(formData.name)
        cmsTemplate.setAuthUserId(request.identity)
        cmsTemplate.setCategoryId(if (formData.categoryId.isDefined) {
          CmsContentCategories.find.byId(formData.categoryId.get.toLong)
        } else {
          new CmsContentCategories
        })
        cmsTemplate.setIsPublished(if (formData.action.eq("save")) {
          YesNoEnum.No
        } else {
          YesNoEnum.Yes
        })
        formData.id match {
          case s: Some[String] =>
            cmsTemplate.setId(s.get.toLong)
            cmsTemplate.update()
          case _ => cmsTemplate.insert()
        }

        //Create the page
        val page = new CmsPages
        page.setAuthUserId(request.identity)
        page.setCmsTemplateId(cmsTemplate)
        page.setInlineCss(formData.inlineCss.orNull)
        page.setInlineJs(formData.inlineJs.orNull)
        page.setSlugUrl(formData.slugUrl)
        page.setIsPublish(formData.action match {
          case "save_publish" => YesNoEnum.Yes
          case _ => YesNoEnum.No
        })
        page.setMetaDescription(formData.metaDesc.orNull)
        page.setMetaKeywords(formData.metaKeywords.orNull)
        page.setName(formData.name)
        page.save()
        //delete all template attr.if exists.
        val attrList = CmsTemplateAttr.find.where.eq("cms_template_id", cmsTemplate.getId).findList()
        if (attrList != null && attrList.size().>=(1)) {
          attrList.asScala.toList.foreach(templateAttr => {
            templateAttr.delete()
          })
        }

        val attrMap = request.body.asFormUrlEncoded.get
        attrMap.foreach { item =>
          if (!item._1.eq("csrfToken")) {
            val cmsTempateAttr = new CmsTemplateAttr
            cmsTempateAttr.setCmsTemplateId(cmsTemplate)
            cmsTempateAttr.setAttr(item._1)
            cmsTempateAttr.setValue(item._2.head)
            cmsTempateAttr.insert()
          }
        }
        Redirect(routes.PageCtrl.index).withNewSession.flashing(("success", "Template Created Successfully."))
      }
    )
  }

  def itemJson(id: Long) = SecuredAction(roles) { implicit request =>
    val data = CmsTemplates.find.byId(id)
    Ok(JsObject(Map("urlPath" -> JsString(data.getCategoryId.getBreadcrumbStr))))
  }

  def delete(id: String) = SecuredAction(roles).async {
    Future.successful {
      CmsTemplates.find.byId(encrypt.decrypt(id).toLong).delete()
      Redirect(routes.PageCtrl.index).withNewSession.flashing(("success", "template deleted successfully."))
    }
  }
}