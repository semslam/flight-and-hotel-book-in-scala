package controllers.admin.systems

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import crypto.Encrypt
import models.{ EmailTemplates, PrivateUsers, Roles, SmsTemplates }
import play.api.data._
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import views.html.admin.systems._
import utils.silhouette.admin.{ AdminController, WithRoles }
import utils.silhouette.MyEnv
import controllers.admin.systems._

import scala.concurrent.Future
import scala.collection.JavaConverters._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.libs.json.{ JsNumber, JsObject, JsString }

/**
 * Created by
 * Igbalajobi Jamiu O. on 01/06/2016 6:44 PM.
 */

class EmailSmsCtrl @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi, implicit val encrypt: Encrypt) extends AdminController {

  val roles = WithRoles(Roles.operation_manager.name(), Roles.admin.name())

  def index = SecuredAction(roles) { implicit request =>
    Ok(emailSms.index(SmsTemplates.find.all().asScala.toList, EmailTemplates.find.all().asScala.toList))
  }

  def emailSmsSetting = SecuredAction(roles) { implicit request =>
    Ok("")
  }

  val emailForm = Form(tuple(
    "id" -> ignored(None: Option[Long]),
    "subject" -> nonEmptyText,
    "message" -> nonEmptyText
  ))

  def createEmailTemplate(id: String) = SecuredAction(roles) { implicit request =>
    if (id != null) {
      val record = EmailTemplates.find.byId(java.lang.Long.parseLong(encrypt.decrypt(id)))
      Ok(emailSms.createEmail(emailForm.fill((Some(record.getId), record.getSubject, record.getMessage))))
    } else
      Ok(emailSms.createEmail(emailForm))
  }

  def saveEmail = SecuredAction(roles) { implicit request =>
    emailForm.bindFromRequest().fold(
      error => BadRequest(emailSms.createEmail(error)),
      success => {
        val emailTemplates = new EmailTemplates
        if (success._1.isDefined) {
          emailTemplates.setId(success._1.get)
        }
        emailTemplates.setName(success._2)
        emailTemplates.setSubject(success._2)
        emailTemplates.setMessage(success._3)
        emailTemplates.save()
        Redirect(routes.EmailSmsCtrl.index).withNewSession.flashing(("success", "Email Template Saved Successfully."))
      }
    )
  }

  def deleteEmailTemplate(id: String) = SecuredAction(roles) { implicit request =>
    val emailTemplates = EmailTemplates.find.byId(java.lang.Long.parseLong(encrypt.decrypt(id)))
    if (emailTemplates != null) {
      emailTemplates.delete()
      Redirect(routes.EmailSmsCtrl.index).withNewSession.flashing(("success", "Email Template Saved Successfully."))
    } else {
      Redirect(routes.EmailSmsCtrl.index)
    }
  }

  val smsForm = Form(tuple(
    "id" -> ignored(None: Option[Long]),
    "category" -> nonEmptyText,
    "name" -> nonEmptyText,
    "message" -> nonEmptyText
  ))

  def createSmsTemplate(id: String) = SecuredAction(roles) { implicit request =>
    if (id != null) {
      val record = SmsTemplates.find.byId(java.lang.Long.parseLong(encrypt.decrypt(id)))
      smsForm.fill((Some(record.getId), record.getCategory, record.getName, record.getMessage))
    }
    Ok(emailSms.createSms(smsForm))
  }

  def deleteSmsTemplate(id: String) = SecuredAction(roles) { implicit request =>
    val smsTemplates = SmsTemplates.find.byId(java.lang.Long.parseLong(encrypt.decrypt(id)))
    if (smsTemplates != null) {
      smsTemplates.delete()
      Redirect(routes.EmailSmsCtrl.index).withNewSession.flashing(("success", "SMS Template Saved Successfully."))
    } else {
      Redirect(routes.EmailSmsCtrl.index)
    }
  }

  def saveSms = SecuredAction(roles) { implicit request =>
    smsForm.bindFromRequest().fold(
      error => BadRequest(emailSms.createSms(error)),
      success => {
        val smsTemplates = new SmsTemplates
        if (success._1.isDefined) {
          smsTemplates.setId(success._1.get)
        }
        smsTemplates.setName(success._3)
        smsTemplates.setMessage(success._4)
        smsTemplates.setCategory(success._2)
        smsTemplates.save()
        Redirect(routes.EmailSmsCtrl.index).withNewSession.flashing(("success", "SMS Template Saved Successfully."))
      }
    )
  }

  def getEmailXhr = SecuredAction(roles) { implicit request =>
    val id = request.getQueryString("id").get
    val ty = request.getQueryString("type").get
    ty match {
      case "email" =>
        val templates = EmailTemplates.find.byId(java.lang.Long.parseLong(id))
        Ok(JsObject(Map(
          "responseCode" -> JsNumber(200),
          "body" -> JsString(templates.getMessage),
          "subject" -> JsString(templates.getSubject)
        )))
      case "sms" =>
        val sms = SmsTemplates.find.byId(java.lang.Long.parseLong(id))
        Ok(JsObject(Map(
          "responseCode" -> JsNumber(200),
          "body" -> JsString(sms.getMessage),
          "subject" -> JsString(sms.getCategory)
        )))
    }
  }

}