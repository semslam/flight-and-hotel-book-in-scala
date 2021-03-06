package controllers.api

import play.api.mvc._
import javax.inject.Inject
import play.api.i18n.{ MessagesApi }

class Application @Inject() (val messagesApi: MessagesApi) extends api.ApiController {

  def test = ApiAction { implicit request =>
    ok("The API is ready")
  }

}
