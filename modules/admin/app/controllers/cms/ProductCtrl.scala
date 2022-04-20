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
import utils.silhouette.admin.{ AdminController, ManagerService, WithRole, WithRoles }
import scala.concurrent.Future
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import controllers.admin.cms.routes

/**
 * Created by
 * Igbalajobi Jamiu O. on 29/05/2016 8:45 AM.
 */

case class ProductDAO(
  id: Option[Long],
  name: String,
  code: String,
  shortDescription: String,
  fullDescription: String,
  pricePerAdult: BigDecimal = BigDecimal(0),
  pricePerChild: BigDecimal = BigDecimal(0),
  pricePerInfant: BigDecimal = BigDecimal(0),
  departingCountries: List[String],
  arrivingCountries: List[String],
  preSelect: Boolean
)

class ProductCtrl @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi, encrypt: Encrypt) extends AdminController {

  val roles = WithRoles(Roles.admin.name(), Roles.operation_manager.name())
  implicit val enc = this.encrypt

  val productForm = Form(mapping(
    "id" -> optional(longNumber),
    "name" -> nonEmptyText,
    "code" -> nonEmptyText,
    "shortDescription" -> nonEmptyText,
    "fullDescription" -> nonEmptyText,
    "pricePerAdult" -> bigDecimal,
    "pricePerChild" -> bigDecimal,
    "pricePerInfant" -> bigDecimal,
    "departingCountries" -> list(text),
    "arrivingCountries" -> list(text),
    "preSelect" -> boolean
  )(ProductDAO.apply)(ProductDAO.unapply))

  def index = SecuredAction(roles) { implicit request =>
    Ok(products.index(AddonProducts.find.all().asScala.toList))
  }

  def create(id: String) = SecuredAction(roles) { implicit request =>
    if (id != null && !id.isEmpty) {
      val dec = encrypt.decrypt(id)
      val data = AddonProducts.find.byId(dec.toLong)

      //      val productDOA = ProductDAO(
      //        id = Option(data.getId)
      //      )
      //      productForm.fill(productDOA)
      Ok(products.create(productForm.fill(ProductDAO(
        id = Some(data.getId),
        name = data.getName,
        code = data.getRefCode,
        shortDescription = data.getShortDesc,
        fullDescription = data.getHtmlDesc,
        pricePerAdult = BigDecimal(data.getAdultPrice),
        pricePerChild = BigDecimal(data.getChildPrice),
        pricePerInfant = BigDecimal(data.getInfantPrice),
        departingCountries = if (data.getDepartureCountryOptions != null) data.getDepartureCountryOptions.split("<<>>").toList else Nil,
        arrivingCountries = if (data.getArrivingCountryOptions != null) data.getArrivingCountryOptions.split("<<>>").toList else Nil,
        preSelect = data.getAutoSelect
      ))))
    } else Ok(products.create(productForm))
  }

  def save = SecuredAction(roles) { implicit request =>
    productForm.bindFromRequest().fold(
      error => {
        //        val data = error.value.get
        //        error.fill(ProductDAO(
        //          id = data.id,
        //          name = data.name,
        //          code = data.code,
        //          shortDescription = data.shortDescription,
        //          fullDescription = data.fullDescription,
        //          pricePerAdult = data.adul,
        //          pricePerChild = BigDecimal(data.getChildPrice),
        //          pricePerInfant = BigDecimal(data.getInfantPrice),
        //          departingCountries = if (data.getDepartureCountryOptions != null) data.getDepartureCountryOptions.split("<<>>").toList else Nil,
        //          arrivingCountries = if (data.getArrivingCountryOptions != null) data.getArrivingCountryOptions.split("<<>>").toList else Nil,
        //          preSelect = data.getAutoSelect
        //        ))
        BadRequest(products.create(error))
      },
      input => {
        val product = new AddonProducts
        product.setName(input.name)
        product.setRefCode(input.code)
        product.setShortDesc(input.shortDescription)
        product.setHtmlDesc(input.fullDescription)
        product.setAdultPrice(input.pricePerAdult.doubleValue())
        product.setChildPrice(input.pricePerChild.doubleValue())
        product.setInfantPrice(input.pricePerInfant.doubleValue())
        product.setAutoSelect(input.preSelect)
        product.setAuthUserId(request.identity)
        if (request.body.asFormUrlEncoded.get.contains("departure_airports[]")) {
          val stringBuilderDepart = new StringBuilder
          request.body.asFormUrlEncoded.get("departure_airports[]").map(a => stringBuilderDepart.append(a + "<<>>"))
          product.setDepartureCountryOptions(stringBuilderDepart.toString())
        }
        if (request.body.asFormUrlEncoded.get.contains("arriving_airports[]")) {
          val stringBuilderArrive = new StringBuilder
          request.body.asFormUrlEncoded.get("arriving_airports[]").map(a => stringBuilderArrive.append(a + "<<>>"))
          product.setArrivingCountryOptions(stringBuilderArrive.toString())
        }
        input.id match {
          case s @ Some(_) =>
            product.setId(s.get); product.update()
          case _ => product.insert()
        }
        Redirect(routes.ProductCtrl.index).withNewSession.flashing(("success", "Product Saved Successfully"))
      }
    )
  }

  def delete(id: String) = SecuredAction(roles).async { implicit request =>
    Future.successful {
      AddonProducts.find.byId(encrypt.decrypt(id).toLong).delete()
      Redirect(routes.ProductCtrl.index).withNewSession.flashing(("success", "Product deleted successfully."))
    }
  }

}
