package controllers.b2b

import javax.inject.{ Inject, Singleton }

import api.ServicesAPI
import com.mohiva.play.silhouette.password.BCryptPasswordHasher
import caching.CacheApi
import com.mohiva.play.silhouette.api.Silhouette
import flight.dto.entity.BookingRequest
import flight.dto.entity.ItineraryWSResponse
import crypto.Encrypt
import models.AppFeatureLibraries._
import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.i18n.MessagesApi
import play.api.libs.json.{ JsNumber, JsObject }
import play.api.mvc._
import utils.silhouette._
import views.html.b2b._
import utils.silhouette.b2b._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class AgentUser(id: Option[String], uid: Option[String], title: String, firstName: String, lastName: String, phone: String, email: String, password: Option[String], rePassword: Option[String], role: String, isPercentage: Boolean, autoApplyMarkup: Boolean, canSetupMarkUpOnFly: Boolean, adt: FareOption, chd: FareOption, inf: FareOption)

case class FareOption(local__return: Option[String], local__oneWay: Option[String], local__multiCity: Option[String], int__return: Option[String], int__oneWay: Option[String], int__multiCity: Option[String])

@Singleton
class UserCtrl @Inject() (val silhouette: Silhouette[MyEnv[Users]], val messagesApi: MessagesApi, encrypt: Encrypt, servicesAPI: ServicesAPI) extends B2BController {

  implicit val cache = servicesAPI.cache
  implicit val enc = encrypt

  val userForm = Form(mapping(
    "id" -> optional(nonEmptyText),
    "uid" -> optional(nonEmptyText),
    "title" -> nonEmptyText,
    "firstName" -> nonEmptyText,
    "lastName" -> nonEmptyText,
    "phone" -> nonEmptyText.verifying(maxLength(11)),
    "email" -> nonEmptyText,
    "password" -> optional(nonEmptyText),
    "rePassword" -> optional(nonEmptyText),
    "role" -> nonEmptyText,
    "isPercentage" -> boolean,
    "autoApplyMarkup" -> boolean,
    "canSetupMarkUpOnFly" -> boolean,
    "adt" -> mapping(
      "local__return" -> optional(nonEmptyText),
      "local__oneWay" -> optional(nonEmptyText),
      "local__multiCity" -> optional(nonEmptyText),
      "int__return" -> optional(nonEmptyText),
      "int__oneWay" -> optional(nonEmptyText),
      "int__multiCity" -> optional(nonEmptyText)
    )(FareOption.apply)(FareOption.unapply),
    "chd" -> mapping(
      "local__return" -> optional(nonEmptyText),
      "local__oneWay" -> optional(nonEmptyText),
      "local__multiCity" -> optional(nonEmptyText),
      "int__return" -> optional(nonEmptyText),
      "int__oneWay" -> optional(nonEmptyText),
      "int__multiCity" -> optional(nonEmptyText)
    )(FareOption.apply)(FareOption.unapply),
    "inf" -> mapping(
      "local__return" -> optional(nonEmptyText),
      "local__oneWay" -> optional(nonEmptyText),
      "local__multiCity" -> optional(nonEmptyText),
      "int__return" -> optional(nonEmptyText),
      "int__oneWay" -> optional(nonEmptyText),
      "int__multiCity" -> optional(nonEmptyText)
    )(FareOption.apply)(FareOption.unapply)
  )(AgentUser.apply)(AgentUser.unapply)
    .verifying("Password not match", data => data.password == data.rePassword)
    .verifying("Oooops! Email already exists", data => Users.find.where().eq("email", data.email).where().eq("id", java.lang.Long.parseLong(data.id.getOrElse("0"))).findRowCount() == 0))

  def index = SecuredAction.async { implicit request =>
    Future.successful(Ok(user.index(request.identity.getAgentCorporateDetailId.getUsersList.asScala.toList)))
  }

  def create(id: String) = SecuredAction { implicit request =>
    if (id != null) {
      val markUp = Users.find.byId(java.lang.Long.parseLong(enc.decrypt(id))).getUserMarkUp
      Ok(user.create(userForm.fill(AgentUser(
        Option(markUp.getId.toString), Option(markUp.getUserId.getId.toString), markUp.getUserId.getPrefix.name, markUp.getUserId.getFirstName, markUp.getUserId.getLastName, markUp.getUserId.getPhone, markUp.getUserId.getEmail, None, None, markUp.getUserId.getRole, markUp.getValueTypes match { case ValueTypes.PERCENTAGE => true case _ => false }, markUp.getUserId.isAutoApplyMarkUp, markUp.getUserId.isCanSetupMarkUpOnFly,
        FareOption(Option(markUp.getLocalReturnAdt.toString), Option(markUp.getLocalOnewayAdt.toString), Option(markUp.getLocalMultiAdt.toString), Option(markUp.getIntReturnAdt.toString), Option(markUp.getIntOnewayAdt.toString), Option(markUp.getIntMultiAdt.toString)),
        FareOption(Option(markUp.getLocalReturnChd.toString), Option(markUp.getLocalOnewayChd.toString), Option(markUp.getLocalMultiChd.toString), Option(markUp.getIntReturnChd.toString), Option(markUp.getIntOnewayChd.toString), Option(markUp.getIntMultiChd.toString)),
        FareOption(Option(markUp.getLocalReturnInf.toString), Option(markUp.getLocalOnewayInf.toString), Option(markUp.getLocalMultiInf.toString), Option(markUp.getIntReturnInf.toString), Option(markUp.getIntOnewayInf.toString), Option(markUp.getIntMultiInf.toString))
      ))))
    } else {
      Ok(user.create(userForm))
    }
  }

  def saveAgent(id: String) = SecuredAction { implicit request =>
    userForm.bindFromRequest().fold(
      error => BadRequest(user.create(error)),
      formValue => {
        val user: Users = formValue.uid match {
          case id: Some[String] => Users.find.byId(java.lang.Long.parseLong(id.get))
          case None if Users.find.where().eq("email", formValue.email).findUnique() == null => new Users()
          case _ => null
        }
        if (user == null) {
          BadRequest(views.html.b2b.user.create(form = userForm.fill(formValue))).withNewSession.flashing(("error", "Email already exists"))
        }
        user.setPrefix(Titles.valueOf(formValue.title))
        user.setFirstName(formValue.firstName)
        user.setLastName(formValue.lastName)
        user.setPhone(formValue.phone)
        formValue.rePassword match {
          case Some(_) => user.setPassword(new BCryptPasswordHasher().hash(formValue.rePassword.get).password)
          case _ =>
        }
        user.setEmail(formValue.email)
        user.setRole(formValue.role)
        user.setAutoApplyMarkUp(formValue.autoApplyMarkup)
        user.setCanSetupMarkUpOnFly(formValue.canSetupMarkUpOnFly)
        user.setAgentCorporateDetailId(request.identity.getAgentCorporateDetailId)
        user.setActivationToken(crypto.Hash.generateSalt())
        user.setIsVerify(YesNoEnum.Yes)
        user.setStatus(models.Status.Active)
        user.setFirstTimeLogin(YesNoEnum.Yes)
        user.setGender(formValue.title match {
          case "Mr" | "Master" => Users.Genders.MALE
          case _ => Users.Genders.FEMALE
        })
        formValue.id match {
          case Some(_) => user.update()
          case None => user.insert()
        }
        val agentMarkUp = new B2bUserMarkUp
        if (formValue.isPercentage) {
          agentMarkUp.setValueTypes(ValueTypes.PERCENTAGE)
        } else {
          agentMarkUp.setValueTypes(ValueTypes.VALUE)
        }
        agentMarkUp.setLocalOnewayAdt(java.lang.Double.parseDouble(formValue.adt.local__oneWay.getOrElse("0.0")))
        agentMarkUp.setLocalReturnAdt(java.lang.Double.parseDouble(formValue.adt.local__return.getOrElse("0.0")))
        agentMarkUp.setLocalMultiAdt(java.lang.Double.parseDouble(formValue.adt.local__multiCity.getOrElse("0.0")))
        agentMarkUp.setIntOnewayAdt(java.lang.Double.parseDouble(formValue.adt.int__oneWay.getOrElse("0.0")))
        agentMarkUp.setIntReturnAdt(java.lang.Double.parseDouble(formValue.adt.int__return.getOrElse("0.0")))
        agentMarkUp.setIntMultiAdt(java.lang.Double.parseDouble(formValue.adt.int__multiCity.getOrElse("0.0")))

        agentMarkUp.setLocalOnewayChd(java.lang.Double.parseDouble(formValue.chd.local__oneWay.getOrElse("0.0")))
        agentMarkUp.setLocalReturnChd(java.lang.Double.parseDouble(formValue.chd.local__return.getOrElse("0.0")))
        agentMarkUp.setLocalMultiChd(java.lang.Double.parseDouble(formValue.chd.local__multiCity.getOrElse("0.0")))
        agentMarkUp.setIntOnewayChd(java.lang.Double.parseDouble(formValue.chd.int__oneWay.getOrElse("0.0")))
        agentMarkUp.setIntReturnChd(java.lang.Double.parseDouble(formValue.chd.int__return.getOrElse("0.0")))
        agentMarkUp.setIntMultiChd(java.lang.Double.parseDouble(formValue.chd.int__multiCity.getOrElse("0.0")))

        agentMarkUp.setLocalOnewayInf(java.lang.Double.parseDouble(formValue.inf.local__oneWay.getOrElse("0.0")))
        agentMarkUp.setLocalReturnInf(java.lang.Double.parseDouble(formValue.inf.local__return.getOrElse("0.0")))
        agentMarkUp.setLocalMultiInf(java.lang.Double.parseDouble(formValue.inf.local__multiCity.getOrElse("0.0")))
        agentMarkUp.setIntOnewayInf(java.lang.Double.parseDouble(formValue.inf.int__oneWay.getOrElse("0.0")))
        agentMarkUp.setIntReturnInf(java.lang.Double.parseDouble(formValue.inf.int__return.getOrElse("0.0")))
        agentMarkUp.setIntMultiInf(java.lang.Double.parseDouble(formValue.inf.int__multiCity.getOrElse("0.0")))

        agentMarkUp.setUserId(user)

        formValue.id match {
          case Some(_) =>
            agentMarkUp.setId(java.lang.Long.parseLong(formValue.id.get))
            agentMarkUp.update()
          case _ => agentMarkUp.insert()
        }
        Redirect(routes.UserCtrl.index()).withNewSession.flashing(("success", "Markup Saved Successfully."))
      }
    )
  }

}