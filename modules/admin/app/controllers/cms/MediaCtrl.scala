package controllers.admin.cms

import java.net.URLEncoder
import javax.inject.Inject

import scala.concurrent.ExecutionContext.Implicits.global
import crypto.Encrypt
import models._
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import utils.silhouette.{ MyEnv }

import scala.collection.JavaConverters._
import scala.concurrent.Future
import views.html.admin.cms._
import controllers.admin.cms._
import aws._
import com.mohiva.play.silhouette.api.Silhouette

import models.admin.MailTokenManager
import utils.silhouette.admin.{ AdminController, ManagerService, WithRole, WithRoles }

/**
 * Created by
 * Igbalajobi Jamiu O. on 01/06/2016 6:49 PM.
 */
class MediaCtrl @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi, encrypt: Encrypt, s3Client: AwsS3Services) extends AdminController {

  val roles = WithRoles(Roles.content_manager.name(), Roles.admin.name())

  def index = SecuredAction(roles).async { implicit request =>
    implicit val encrypt = this.encrypt
    Future.successful {
      Ok(media.index(Medias.find.all().asScala.toList))
    }
  }

  def xhrUpload = Action.async(Future.successful {
    Ok("Uploading...")
  })

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

  def upload = Action.async { implicit request =>
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
            uploadedFile
          }))
        }
      }
      Redirect(routes.MediaCtrl.index).withNewSession.flashing(("success", "File Uploaded Successfully"))
    }
  }

  def delete(id: String) = SecuredAction(roles).async { implicit request =>
    val media = Medias.find.byId(encrypt.decrypt(id).toLong)
    s3Client.s3FilesClient.delete(media).map(unit => {
      media.delete()
      Redirect(routes.MediaCtrl.index).withNewSession.flashing(("success", "File Deleted Successfully"))
    })
  }
}
