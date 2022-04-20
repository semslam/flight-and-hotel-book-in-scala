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
import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

/**
 * Created by
 * Igbalajobi Jamiu O. on 29/05/2016 8:45 AM.
 */

case class PackageThemeDAO(id: Option[String], name: String, desc: Option[String])

case class PackageAirportOptionDAO(
  id: Option[Long],
  originAirport: Option[Long],
  arrivingAirport: Option[Long],
  airlineId: Option[Long],
  tripType: Option[String],
  cabinClass: Option[String],
  earlierDepartureDate: Option[String],
  earlierArrivalDate: Option[String],
  duration: Option[Int],
  priceDescription: Option[String],
  description: Option[String]
)

case class PackageItineraryDAO(id: Option[Long], name: Option[String], imageURL: Option[String], description: Option[String], inclusion: List[Option[String]])

case class PackageHotelOptionDAO(
  id: Option[Long],
  hotelName: Option[String],
  description: Option[String],
  imageURL: Option[String],
  hotelRating: Option[Int],
  roomName: Option[String]
)

case class PackageDOA(
  id: Option[String],
  theme: Option[Long],
  name: String,
  code: String,
  shortDescription: Option[String],
  description: Option[String],
  thumbImageUrl: Option[String],
  packageRating: Option[Int],
  adultUnitPrice: Option[Int],
  childUnitPrice: Option[Int],
  isRefundable: Boolean,
  includeFlight: Boolean,
  includeHotel: Boolean,
  packageDestination: Long,
  policy: Option[String],
  facilities: List[Option[String]],
  packageInclusions: List[Option[String]],
  packageExclusions: List[Option[String]],
  packageItinerary: List[PackageItineraryDAO],
  packageImages: List[Option[String]],
  airportOption: Option[PackageAirportOptionDAO],
  hotelOption: Option[PackageHotelOptionDAO]
)

class PackageCtrl @Inject() (val silhouette: Silhouette[MyEnv[PrivateUsers]], val messagesApi: MessagesApi, encrypt: Encrypt) extends AdminController {

  val roles = WithRoles(Roles.content_manager.name())
  implicit val enc = this.encrypt

  val packageForm = Form(mapping(
    "id" -> optional(nonEmptyText),
    "theme" -> optional(longNumber),
    "name" -> nonEmptyText,
    "code" -> nonEmptyText,
    "shortDescription" -> optional(nonEmptyText),
    "description" -> optional(nonEmptyText),
    "thumbImageUrl" -> optional(nonEmptyText),
    "packageRating" -> optional(number),
    "adultUnitPrice" -> optional(number),
    "childUnitPrice" -> optional(number),
    "isRefundable" -> boolean,
    "includeFlight" -> boolean,
    "includeHotel" -> boolean,
    "packageDestination" -> longNumber.verifying("Specified city doesn't exist. Please enter a valid city code", id => Countries.find.byId(id) != null),
    "policy" -> optional(nonEmptyText),
    "facilities" -> list(optional(nonEmptyText)),
    "packageInclusions" -> list(optional(nonEmptyText)),
    "packageExclusions" -> list(optional(nonEmptyText)),
    "packageItinerary" -> list(mapping(
      "id" -> optional(longNumber),
      "name" -> optional(nonEmptyText),
      "imageURL" -> optional(nonEmptyText),
      "description" -> optional(nonEmptyText),
      "inclusion" -> list(optional(nonEmptyText))
    )(PackageItineraryDAO.apply)(PackageItineraryDAO.unapply)),
    "packageImages" -> list(optional(nonEmptyText)),
    "airportOption" -> optional(mapping(
      "id" -> optional(longNumber),
      "originAirport" -> optional(longNumber),
      "arrivingAirport" -> optional(longNumber),
      "airlineId" -> optional(longNumber),
      "tripType" -> optional(nonEmptyText),
      "cabinClass" -> optional(nonEmptyText),
      "earlierDepartureDate" -> optional(nonEmptyText),
      "earlierArrivalDate" -> optional(nonEmptyText),
      "duration" -> optional(number),
      "priceDescription" -> optional(nonEmptyText),
      "description" -> optional(nonEmptyText)
    )(PackageAirportOptionDAO.apply)(PackageAirportOptionDAO.unapply)),
    "hotelOption" -> optional(mapping(
      "id" -> optional(longNumber),
      "hotelName" -> optional(nonEmptyText),
      "description" -> optional(nonEmptyText),
      "imageURL" -> optional(nonEmptyText),
      "hotelRating" -> optional(number),
      "roomName" -> optional(nonEmptyText)
    )(PackageHotelOptionDAO.apply)(PackageHotelOptionDAO.unapply))
  )(PackageDOA.apply)(PackageDOA.unapply))

  val packageTheme = Form(mapping(
    "id" -> optional(nonEmptyText),
    "name" -> nonEmptyText,
    "desc" -> optional(nonEmptyText)
  )(PackageThemeDAO.apply)(PackageThemeDAO.unapply))

  def index = SecuredAction(roles) { implicit request =>
    Ok(packages.index(Packages.find.all().asScala.toList))
  }

  def create(id: String) = SecuredAction(roles) { implicit request =>
    if (id != null && !id.isEmpty) {
      val dec = encrypt.decrypt(id)
      val data = Packages.find.byId(dec.toLong)
      val packageDOA = PackageDOA(
        id = Option(data.getId.toString),
        Option(data.getPackageThemeId.getId),
        data.getName,
        data.getCode,
        Option(data.getShortDescription),
        Option(data.getDescription),
        Option(data.getThumbImageUrl),
        Option(data.getPackageRating),
        Option(data.getAdultUnitPrice.toInt),
        Option(data.getChildUnitPrice.toInt),
        data.getRefundable,
        data.getIncludeFlight,
        data.getIncludeHotel,
        data.getCountryId.getId,
        Option(data.getPolicy),
        List[Option[String]](),
        data.getPackageInclusions.map(a => Option(a.getIncludedItem)).toList,
        data.getPackageExclusions.map(a => Option(a.getExcludedItem)).toList,
        data.getPackageItineraries.map(item => PackageItineraryDAO(
          Option(item.getId),
          Option(item.getName),
          Option(item.getImageUrl),
          Option(item.getDescription),
          item.getPackageItineraryIncludes.map(a => Option(a.getIncludedItem)).toList
        )).toList,
        data.getPackageImages.map(img => Option(img.getImageUrl)).toList,
        data.getPackageFlightOption.map(item => PackageAirportOptionDAO(
          Option(item.getId),
          Option(item.getOriginAirport.getId),
          Option(item.getDestinationAirport.getId),
          Option(item.getAirlineId.getId),
          Option(item.getTripType.name()),
          Option(item.getCabinClass.name()),
          Option(item.getDepartureDate),
          Option(item.getReturningDate),
          Option(item.getDuration),
          Option(item.getPriceDescription.toString),
          Option(item.getDescription)
        )).headOption,
        None
      )
      Ok(packages.create(packageForm.fill(packageDOA)))
    } else Ok(packages.create(packageForm))
  }

  def save = SecuredAction(roles) { implicit request =>
    packageForm.bindFromRequest().fold(
      error => {
        error.errors.map(println)
        BadRequest(packages.create(error))
      },
      input => {
        val packages = new Packages
        packages.setAuthUserId(request.identity)
        if (input.theme.nonEmpty) packages.setPackageThemeId(PackageTheme.find.byId(input.theme.get))
        packages.setCountryId(Countries.find.byId(input.packageDestination))
        packages.setName(input.name)
        packages.setCode(input.code)
        packages.setThumbImageUrl(input.thumbImageUrl.orNull)
        packages.setDescription(input.description.orNull)
        packages.setShortDescription(input.shortDescription.orNull)
        packages.setAdultUnitPrice(input.adultUnitPrice.getOrElse(0).toDouble)
        packages.setChildUnitPrice(input.childUnitPrice.getOrElse(0).toDouble)
        packages.setRefundable(input.isRefundable)
        packages.setPackageRating(input.packageRating.getOrElse(0).toInt)
        packages.setIncludeFlight(input.includeFlight)
        packages.setIncludeHotel(input.includeHotel)
        packages.setPolicy(input.policy.orNull)
        input.id match {
          case s: Some[String] =>
            packages.setId(java.lang.Long.parseLong(encrypt.decrypt(s.get))); packages.update()
          case _ => packages.insert()
        }
        val packageInclusion = PackageInclusions.find.where().eq("packageId", packages)
        val packageExclusions = PackageExclusions.find.where().eq("packageId", packages)
        val packageImages = PackageImages.find.where().eq("packageId", packages)
        val packageItineraries = PackageItinerary.find.where().eq("packageId", packages)
        if (packageInclusion.findRowCount() != 0) packageInclusion.findList().foreach { item =>
          item.setDeleted(true)
          item.setDeletedAt(new java.util.Date())
          item.update()
        }
        if (packageExclusions.findRowCount() != 0) packageExclusions.findList().foreach { item =>
          item.setDeleted(true)
          item.setDeletedAt(new java.util.Date())
          item.update()
        }
        if (packageImages.findRowCount() != 0) packageImages.findList().foreach { item =>
          item.setDeleted(true)
          item.setDeletedAt(new java.util.Date())
          item.update()
        }
        if (packageItineraries.findRowCount() != 0) packageItineraries.findList().foreach { item =>
          item.setDeleted(true)
          item.setDeletedAt(new java.util.Date())
          item.update()
        }

        input.packageInclusions.foreach { inclusion =>
          val packageInclusion = new PackageInclusions
          packageInclusion.setIncludedItem(inclusion.orNull)
          packageInclusion.setPackageId(packages)
          packageInclusion.insert()
        }
        input.packageExclusions.foreach { exclusion =>
          val packageExclusion = new PackageExclusions
          packageExclusion.setExcludedItem(exclusion.orNull)
          packageExclusion.setPackageId(packages)
          packageExclusion.insert()
        }
        input.packageImages.foreach { image =>
          val packageImages = new PackageImages
          packageImages.setImageUrl(image.orNull)
          packageImages.setPackageId(packages)
          packageImages.insert()
        }
        var day = 1
        input.packageItinerary.foreach { itinerary =>
          val packageItinerary = new PackageItinerary
          packageItinerary.setDay(day.toString)
          packageItinerary.setDescription(itinerary.description.orNull)
          packageItinerary.setPackageId(packages)
          packageItinerary.setImageUrl(itinerary.imageURL.orNull)
          packageItinerary.setName(itinerary.name.orNull)
          packageItinerary.insert()
          val packageItineraryInclusions = PackageItineraryIncludes.find.where().eq("packageItineraryId", packageItinerary)
          if (packageItineraryInclusions.findRowCount() > 0) packageItineraryInclusions.findList().foreach { item =>
            item.setDeleted(true)
            item.setDeletedAt(new java.util.Date())
            item.update()
          }
          itinerary.inclusion.foreach { inclusion =>
            val packageItineraryIncludes = new PackageItineraryIncludes
            packageItineraryIncludes.setPackageItineraryId(packageItinerary)
            packageItineraryIncludes.setIncludedItem(inclusion.orNull)
            packageItineraryIncludes.insert()
          }

          day += 1
        }
        if (input.includeFlight && input.airportOption.nonEmpty) {
          //Save the flight record
          val flightInput = input.airportOption.get
          val packageFlightOption = new PackageFlightOption
          if (flightInput.originAirport.nonEmpty) packageFlightOption.setOriginAirport(Airports.find.byId(flightInput.originAirport.get))
          if (flightInput.arrivingAirport.nonEmpty)
            packageFlightOption.setDestinationAirport(Airports.find.byId(flightInput.arrivingAirport.get))
          if (flightInput.airlineId.nonEmpty) packageFlightOption.setAirlineId(Airlines.find.byId(flightInput.airlineId.get))
          if (flightInput.tripType.nonEmpty)
            packageFlightOption.setTripType(TripType.valueOf(flightInput.tripType.get))
          if (flightInput.cabinClass.nonEmpty) packageFlightOption.setCabinClass(CabinClass.valueOf(flightInput.cabinClass.get))
          if (flightInput.earlierDepartureDate.nonEmpty) packageFlightOption.setDepartureDate(flightInput.earlierDepartureDate.get)
          if (flightInput.earlierArrivalDate.nonEmpty) packageFlightOption.setReturningDate(flightInput.earlierArrivalDate.get)
          if (flightInput.duration.nonEmpty) packageFlightOption.setDuration(flightInput.duration.get)
          if (flightInput.description.nonEmpty) packageFlightOption.setDescription(flightInput.description.get)
          flightInput.id match {
            case s @ Some(_) =>
              packageFlightOption.setId(s.get); packageFlightOption.update()
            case _ => packageFlightOption.insert()
          }
        }
        if (input.includeHotel && input.hotelOption.nonEmpty) {
          val hotelInput = input.hotelOption.get
          val packageHotelOption = new PackageHotelOption
          packageHotelOption.setHotelName(hotelInput.hotelName.orNull)
          packageHotelOption.setImageURL(hotelInput.imageURL.orNull)
          packageHotelOption.setRoomName(hotelInput.roomName.orNull)
          packageHotelOption.setDescription(hotelInput.description.orNull)
          hotelInput.id match {
            case s @ Some(_) =>
              packageHotelOption.setId(s.get); packageHotelOption.update()
            case _ => packageHotelOption.insert()
          }
        }
        Redirect(routes.PackageCtrl.index).withNewSession.flashing(("success", "Package Saved Successfully"))
      }
    )
  }

  def delete(id: String) = SecuredAction(roles).async { implicit request =>
    Future.successful {
      Packages.find.byId(encrypt.decrypt(id).toLong).delete()
      Redirect(routes.PackageCtrl.index).withNewSession.flashing(("success", "Package deleted successfully."))
    }
  }

  def createTheme(id: String) = SecuredAction(roles) { implicit request =>
    if (id != null && !id.isEmpty) {
      val data = PackageTheme.find.byId(encrypt.decrypt(id).toLong)
      val packageDOA = PackageThemeDAO(Option(data.getId.toString), data.getName, Option(data.getDescription))
      Ok(packages.createTheme(packageTheme.fill(packageDOA)))
    } else Ok(packages.createTheme(packageTheme))
  }

  def manageTheme = SecuredAction(roles) { implicit request =>
    Ok(packages.manageTheme(PackageTheme.find.all().asScala.toList))
  }

  def saveTheme = SecuredAction(roles) { implicit request =>
    packageTheme.bindFromRequest().fold(
      error => BadRequest(packages.createTheme(error)),
      input => {
        val packages = new PackageTheme
        packages.setName(input.name)
        packages.setDescription(input.desc.orNull)
        packages.setPrivateUserId(request.identity)
        input.id match {
          case s: Some[String] =>
            packages.setId(encrypt.decrypt(s.get).toLong); packages.update()
          case _ => packages.insert()
        }
      }
    )
    Redirect(routes.PackageCtrl.manageTheme()).withNewSession.flashing(("success", "Package Theme saved successfully."))
  }

  def deleteTheme(id: String) = SecuredAction(roles) { implicit request =>
    PackageTheme.find.byId(encrypt.decrypt(id).toLong).delete()
    Redirect(routes.PackageCtrl.manageTheme()).withNewSession.flashing(("success", "Package Theme deleted successfully."))
  }

}
