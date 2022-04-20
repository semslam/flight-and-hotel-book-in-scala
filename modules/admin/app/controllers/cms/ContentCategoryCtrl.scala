package controllers.admin.cms

import crypto.Encrypt
import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.libs.json.{ JsNumber, JsObject, JsString }
import utils.silhouette._
import play.api._
import play.api.mvc._
import play.api.i18n.{ Lang, Messages, MessagesApi }

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import views.html.admin.cms._
import controllers.admin.cms._
import models.admin.MailTokenManager
import utils.silhouette.admin.{ AdminController, ManagerService, WithRole, WithRoles }

/**
 * Created by
 * Igbalajobi Jamiu O. on 26/05/2016 6:06 PM.
 */
case class ContentDOA(id: Option[String], parent_id: Option[String], name: String)

class ContentCategoryCtrl @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi, encrypt: Encrypt) extends AdminController {
  val roles = WithRoles(Roles.content_manager.name())
  implicit val enc: Encrypt = this.encrypt

  val contentCrudForm = Form(mapping(
    "id" -> optional(nonEmptyText),
    "parent_id" -> optional(nonEmptyText),
    "name" -> nonEmptyText.verifying(minLength(2))
  )(ContentDOA.apply)(ContentDOA.unapply))

  def index = SecuredAction(roles).async { implicit request =>
    Future.successful(Ok(content_categories.index(CmsContentCategories.find.orderBy("id desc").findList.asScala.toList)))
  }

  def create(id: String) = SecuredAction(roles).async { implicit request =>
    Future.successful {
      if (id != null && !id.isEmpty) {
        val dec = encrypt.decrypt(id)
        val data = CmsContentCategories.find.byId(dec.toLong)
        val parentId: Option[String] = if (data.getParentId == null) { None } else { Some(data.getParentId.getId.toString) }
        Ok(content_categories.create(contentCrudForm.fill(ContentDOA(Some(data.getId.toString), parentId, data.getName))))
      } else Ok(content_categories.create(contentCrudForm))
    }
  }

  def save = SecuredAction(roles) { implicit request =>
    contentCrudForm.bindFromRequest.fold(
      error => BadRequest(content_categories.create(error)),
      cmsContenForm => {
        val contentCat = new CmsContentCategories
        if (cmsContenForm.id.isDefined) {
          contentCat.setId(java.lang.Long.parseLong(cmsContenForm.id.get))
        }
        contentCat.setAuthUserId(request.identity) //request.identity.id
        if (cmsContenForm.parent_id.isDefined) {
          val cmsContentCat = CmsContentCategories.find.byId(java.lang.Long.parseLong(cmsContenForm.parent_id.get))
          contentCat.setBreadcrumbStr(s"${cmsContentCat.getBreadcrumbStr}/${cmsContenForm.name}")
          contentCat.setParentId(cmsContentCat)
        } else {
          contentCat.setBreadcrumbStr("/".concat(cmsContenForm.name))
        }
        contentCat.setName(cmsContenForm.name)
        cmsContenForm.id match {
          case s: Some[String] => contentCat.update()
          case None => contentCat.insert()
        }
        Redirect(routes.ContentCategoryCtrl.index).withNewSession.flashing(("success", "New content category saved successfully."))
      }
    )
  }

  def delete(id: String) = SecuredAction(WithRole("content_manager")) { implicit request =>
    val decId = encrypt.decrypt(id).toLong
    CmsContentCategories.find.byId(decId).delete()
    Redirect(routes.ContentCategoryCtrl.index).withNewSession.flashing(("success", "content category deleted successfully."))
  }
}
