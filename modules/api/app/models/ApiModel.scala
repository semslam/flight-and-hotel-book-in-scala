package models.api

import api.Page
import java.text.SimpleDateFormat

import org.joda.time.DateTime

import scala.collection.mutable.Map

/*
* A fake DB to store and load all the data
*/
object ApiModel {

  val dt = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss")

  // API KEYS
  val apiKeys = models.ApiToken.find

  // TOKENS
  val tokens = models.ApiToken.find

  // API REQUEST LOG
  val logs = FakeTable[ApiLog]()

  /*
	* Fake table that emulates a SQL table with an auto-increment index
	*/
  case class FakeTable[A](var table: Map[Long, A], var incr: Long) {
    def nextId: Long = {
      if (!table.contains(incr))
        incr
      else {
        incr += 1
        nextId
      }
    }
    def get(id: Long): Option[A] = table.get(id)
    def find(p: A => Boolean): Option[A] = table.values.find(p)
    def insert(a: Long => A): (Long, A) = {
      val id = nextId
      val tuple = (id -> a(id))
      table += tuple
      incr += 1
      tuple
    }
    def update(id: Long)(f: A => A): Boolean = {
      get(id).map { a =>
        table += (id -> f(a))
        true
      }.getOrElse(false)
    }
    def delete(id: Long): Unit = table -= id
    def delete(p: A => Boolean): Unit = table = table.filterNot { case (id, a) => p(a) }

    def values: List[A] = table.values.toList
    def map[B](f: A => B): List[B] = values.map(f)
    def filter(p: A => Boolean): List[A] = values.filter(p)
    def exists(p: A => Boolean): Boolean = values.exists(p)
    def count(p: A => Boolean): Int = values.count(p)
    def size: Int = table.size
    def isEmpty: Boolean = size == 0

    def page(p: Int, s: Int)(filterFunc: A => Boolean)(sortFuncs: ((A, A) => Boolean)*): Page[A] = {
      val items = filter(filterFunc)
      val sorted = sortFuncs.foldRight(items)((f, items) => items.sortWith(f))
      Page(
        items = sorted.drop((p - 1) * s).take(s),
        page = p,
        size = s,
        total = sorted.size
      )
    }
  }

  object FakeTable {
    def apply[A](elements: (Long, A)*): FakeTable[A] = apply(Map(elements: _*), elements.size + 1)
  }

}
