package models

import javax.inject.{ Inject, Singleton }

import api.ServicesAPI
import com.avaje.ebean.Ebean
import java._

import scala.collection.JavaConverters._

/**
 * Created by Igbalajobi Jamiu Okunade on 6/9/17.
 */
@Singleton
class SqlQueryCached @Inject() (serviceAPI: ServicesAPI) {

  lazy val cache = serviceAPI.cache

  def getQueryResults[T](queryClass: Class[T]): List[T] = cache.getItem[util.List[T]](queryClass.getClass.getName).getOrElse {
    val result = Ebean.createQuery(queryClass).findList()
    cache.setItem(queryClass.getClass.getName, result)
    result
  }.asScala.toList

}
