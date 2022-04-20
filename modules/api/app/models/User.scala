package models.api

import models.Users

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class User(
  id: Long,
  email: String,
  password: String,
  name: String,
  emailConfirmed: Boolean,
  active: Boolean
)

object User {

  val userQuery = Users.find

  def findById(id: Long): Future[Option[Users]] = Future.successful {
    Option(userQuery.byId(id))
  }

  def findByEmail(email: String): Future[Option[Users]] = Future.successful {
    Option(userQuery.where().eq("email", email).findUnique())
  }

  def insert(email: String, password: String, name: String): Future[(Long, Users)] = Future.successful {
    val user = new Users
    (user.getId, user)
    //    users.insert(User(_, email, password, name, emailConfirmed = false, active = false))
  }

  def update(id: Long, name: String): Future[Boolean] = Future.successful {
    val user = userQuery.byId(id)
    user.setFirstName(name)
    user.update()
    true
    //    users.update(id)(_.copy(name = name))
  }

  def confirmEmail(id: Long): Future[Boolean] = Future.successful {

    //    users.update(id)(_.copy(emailConfirmed = true, active = true))
    true
  }

  def updatePassword(id: Long, password: String): Future[Boolean] = Future.successful {
    //    users.update(id)(_.copy(password = password))
    true
  }

  def delete(id: Long): Future[Unit] = Future.successful {
    //    FakeDB.folders.map(f => FakeDB.tasks.delete(_.folderId == f.id))
    //    FakeDB.folders.delete(_.userId == id)
    //    users.delete(id)

  }

  def list: Future[Seq[Users]] = Future.successful {
    import scala.collection.JavaConversions._
    userQuery.all().toSeq.sortBy(_.getId)
  }

}
