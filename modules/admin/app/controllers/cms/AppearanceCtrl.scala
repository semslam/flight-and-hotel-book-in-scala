package controllers.admin.cms

import java.net.URLEncoder
import java.util
import javax.inject.Inject

import com.alajobi.ota.utils.SystemControlSetting
import com.alajobi.ota.utils.SystemControlSetting._
import aws._
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import com.alajobi.ota.utils.SystemControlSetting
import crypto.Encrypt
import models.{ MediaSourceTypes, Medias, PrivateUsers, Roles }
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.data._
import play.api.i18n.MessagesApi
import play.api.mvc.{ AnyContent, RequestHeader }
import play.twirl.api.Html
import utils.silhouette.MyEnv
import views.html.admin.cms._

import models.admin.MailTokenManager
import utils.silhouette.admin.{ AdminController, ManagerService, WithRole, WithRoles }
import scala.concurrent.Future
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by
 * Igbalajobi Jamiu O. on 01/06/2016 6:49 PM.
 */
class AppearanceCtrl @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi, encrypt: Encrypt, s3Client: AwsS3Services) extends AdminController {

  val roles = WithRoles(Roles.content_manager.name())
  implicit val enc = this.encrypt
  lazy val systemControlSetting = SystemControlSetting.getInstance()

  val headerForm = Form(tuple(
    "header" -> text,
    "headerTags" -> optional(text),
    "headerMobileTags" -> optional(text),
    "tabTitle" -> optional(text),
    "seoKeywords" -> optional(text),
    "seoDescription" -> optional(text)
  ))

  val footerForm = Form(tuple(
    "footer" -> nonEmptyText,
    "footerTags" -> optional(nonEmptyText)
  ))

  val logoForm = Form(nonEmptyText)

  val searchingForm = Form(tuple(
    "flightLoadHtml" -> optional(nonEmptyText),
    "hotelLoadHtml" -> optional(nonEmptyText),
    "flightHotelLoadHtml" -> optional(nonEmptyText),
    "cms_load_background" -> optional(nonEmptyText)
  ))

  private def view(implicit securedRequest: SecuredRequest[MyEnv[PrivateUsers], AnyContent]): Html = {
    val fileUrl = systemControlSetting.get(CMS_LOGO)
    appearance.index(
      headerForm.fill(
        systemControlSetting.get(CMS_HTML_HEADER),
        Option(systemControlSetting.get(CMS_HTML_HEADER_META_TAG)),
        Option(systemControlSetting.get("cms_page_footer_mobile_tag")),
        Option(systemControlSetting.get(SystemControlSetting.CMS_TAB_TITLE)),
        Option(systemControlSetting.get(SystemControlSetting.CMS_SEO_META_KEYWORD)),
        Option(systemControlSetting.get(SystemControlSetting.CMS_SEO_META_DESCRIPTION))
      ),
      footerForm.fill(systemControlSetting.get(CMS_HTML_FOOTER), Option(systemControlSetting.get(CMS_HTML_FOOTER_META_TAG))),
      logoForm.fill(fileUrl),
      searchingForm.fill(Option(systemControlSetting.get(CMS_FLIGHT_PRELOADER_HTML)), Option(systemControlSetting.get(CMS_HOTEL_PRELOADER_HTML)), Option(systemControlSetting.get(CMS_FLIGHT_HOTEL_PRELOADER_HTML)), Option(systemControlSetting.get("cms_load_background")))
    )
  }

  def index = SecuredAction(roles).async { implicit request =>
    Future.successful(Ok(view))
  }

  def header = SecuredAction(roles) { implicit request =>
    headerForm.bindFromRequest().fold(
      error => BadRequest(view),
      formValue => {
        val items = new util.HashMap[String, String]()
        items.put(CMS_HTML_HEADER, formValue._1)
        items.put(CMS_HTML_HEADER_META_TAG, formValue._2.getOrElse(""))
        items.put("cms_page_footer_mobile_tag", formValue._3.getOrElse(""))
        items.put(CMS_TAB_TITLE, formValue._4.getOrElse(""))
        items.put(CMS_SEO_META_KEYWORD, formValue._5.getOrElse(""))
        items.put(CMS_SEO_META_DESCRIPTION, formValue._6.getOrElse(""))
        systemControlSetting.put(items)
        Redirect(routes.AppearanceCtrl.index).withNewSession.flashing(("success", "Header Saved Successfully."))
      }
    )
  }

  def footer = SecuredAction(roles) { implicit request =>
    footerForm.bindFromRequest().fold(
      error => BadRequest(view),
      formValue => {
        val items = new util.HashMap[String, String]()
        items.put(CMS_HTML_FOOTER, formValue._1)
        items.put(CMS_HTML_FOOTER_META_TAG, formValue._2.getOrElse(""))
        systemControlSetting.put(items)
        Redirect(routes.AppearanceCtrl.index).withNewSession.flashing(("success", "Footer Saved Successfully."))
      }
    )
  }

  def logo = SecuredAction(roles).async { implicit request =>
    Future.successful {
      if (request.body.asMultipartFormData.isDefined) {
        request.body.asMultipartFormData.get.files.map { file =>
          s3Client.s3FilesClient.uploadFile(file).map(_.map(uploadedFile => {
            //save the record to the database.
            val db = new Medias
            db.setSize(file.ref.file.length.toString)
            db.setFileUrl(s"${s3Client.awsAuth.endpointUrl}${URLEncoder.encode(file.filename, "UTF-8")}")
            db.setFileName(file.filename)
            db.setBucketKey(s3Client.awsAuth.defaultBucket)
            db.setSourceType(MediaSourceTypes.aws)
            db.setExt(getExtension(file.contentType.getOrElse("")))
            db.save()
            systemControlSetting.put(CMS_LOGO, db.getFileUrl)
            uploadedFile
          }))
        }
      }
      Redirect(routes.AppearanceCtrl.index).withNewSession.flashing(("success", "Logo Changed Successfully"))
    }
  }

  def getExtension(contentType: String): String = {
    val charSequence = contentType.subSequence(1, 3)
    var ret = ""
    if (contentType.matches("/jpeg/") || contentType.matches("/jpg/")) {
      ret = "jpeg"
    } else if (contentType.matches("/png/")) {
      ret = "png"
    } else if (contentType.matches("/gif/")) {
      ret = "gif"
    } else if (contentType.matches("pdf")) {
      ret = "pdf"
    } else {
      ret = "file"
    }
    ret
  }

  def searching = SecuredAction(roles) { implicit request =>
    searchingForm.bindFromRequest().fold(
      error => BadRequest(view),
      formValue => {
        val items = new util.HashMap[String, String]()
        items.put(CMS_FLIGHT_PRELOADER_HTML, formValue._1.getOrElse(""))
        items.put(CMS_HOTEL_PRELOADER_HTML, formValue._2.getOrElse(""))
        items.put(CMS_FLIGHT_HOTEL_PRELOADER_HTML, formValue._3.getOrElse(""))
        items.put("cms_load_background", formValue._4.getOrElse(""))
        systemControlSetting.put(items)
        Redirect(routes.AppearanceCtrl.index).withNewSession.flashing(("success", "Saved Successfully."))
      }
    )
  }

}
