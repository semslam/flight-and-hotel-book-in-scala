import java.util.Date
import javax.inject.{ Inject, Named, Singleton }

import api.ServicesAPI
import aws.AwsS3Services
import caching.CacheApi
import caching.engine.ehcache.EHCacheEngineImpl
import caching.engine.memcache.MemCacheEngineImpl
import caching.engine.redis.RedisEngineImpl
import com.avaje.ebean.{ Ebean, EbeanServer }
import com.google.inject._
import com.google.inject.name.Names
import flight.dto.entity.{ PNRModifyRequest }
import mailer.MailService
import models.BookingStatus._
import models._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.{ Configuration, Environment }
import utils._
import scala.collection.JavaConversions._
import scala.concurrent.duration._
import java.util.{ Date, Calendar }
import java.text.SimpleDateFormat
import akka.actor.ActorSystem

/**
 * Created by
 * Igbalajobi Jamiu O. on 02/04/2017 12:30 PM.
 */

class OnStartUp(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure() = {
    bind(classOf[CacheApi]).annotatedWith(Names.named("PlayCache")).to(classOf[EHCacheEngineImpl]).asEagerSingleton()
    // bind(classOf[CacheApi]).annotatedWith(Names.named("MemCache")).to(classOf[MemCacheEngineImpl]).asEagerSingleton()
    //    bind(classOf[CacheApi]).annotatedWith(Names.named("Redis")).to(classOf[RedisEngineImpl]).asEagerSingleton()
    bind(classOf[OnStartFactory]).asEagerSingleton()
    bind(classOf[AwsS3Services]).asEagerSingleton()
  }

}

@Singleton
class OnStartFactory @Inject() (implicit serviceApi: ServicesAPI, implicit val mailService: MailService) {

  println("OnStartUp")

  implicit lazy val actorSystem = ActorSystem("travelden")

  val cacheableModels = Array(classOf[Airlines], classOf[Airports], classOf[Countries], classOf[HotelDestinations])

  cacheableModels.foreach { model =>
    serviceApi.cache.setItem(model.getClass.getName.toLowerCase, Ebean.createQuery(model).findList())
  }

  //Asynchronously check if PNR is still valid for bookings and update PNR
  actorSystem.scheduler.schedule(0 hour, 20 seconds, new Runnable {
    override def run(): Unit = {
      // val flightAvailabilityRequest = FlightAvailabilityRequest()
      // serviceApi.flightApi.readPNR(???)
    }
  })

  /**
   * Payment Reminder Auto-Reminder
   */
  actorSystem.scheduler.schedule(1 hour, 4 hours, new Runnable {
    override def run(): Unit = {
      val dtFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
      val last10Hrs = dtFormat.format(new Date(System.currentTimeMillis - (13 hours).toMillis))
      val last12Hrs = dtFormat.format(new Date(System.currentTimeMillis - (1 hours).toMillis))
      val calendar = Calendar.getInstance()
      if (calendar.get(Calendar.HOUR_OF_DAY) <= 22) {
        val pendingBookings = FlightBookings.find
          .where().between("bookings.createdAt", last10Hrs, last12Hrs)
          .where().eq("bookings.status", BookingStatus.CONFIRMED)
          .where().eq("bookings.paymentHistoryId.status", PaymentStatus.Pending)
          .where().eq("bookings.isArchived", false)
          .findList()
        pendingBookings.foreach { flightBooking =>
          println(s"Payment Reminder Sent to ${flightBooking.getBookings.getTransactionRef}\n")
          Mailer.paymentReminder(flightBooking)
        }
      }
    }
  })

}
