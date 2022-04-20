import play.api.http.DefaultHttpErrorHandler
import com.mohiva.play.silhouette.api.actions.{ SecuredErrorHandler, UnsecuredErrorHandler }
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.routing.Router
import scala.concurrent.Future
import javax.inject.{ Singleton, Inject, Provider }

@Singleton
class ErrorHandler @Inject() (
    env: Environment,
    config: Configuration,
    sourceMapper: OptionalSourceMapper,
    router: Provider[Router],
    webErrorHandler: web.ErrorHandler,
    adminErrorHandler: admin.ErrorHandler,
    b2bErrorHandler: b2b.ErrorHandler,
    apiErrorHandler: api.utils.ErrorHandler
) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) with SecuredErrorHandler with UnsecuredErrorHandler {

  /*
	* Gets the subdomain: "admin" o "b2b" o "www"
	*/
  private def getSubdomain(request: RequestHeader) = request.domain.replaceFirst("[\\.]?[^\\.]+[\\.][^\\.]+$", "")

  // 401 - Unauthorized
  override def onNotAuthenticated(implicit request: RequestHeader) = getSubdomain(request) match {
    case "admin" => adminErrorHandler.onNotAuthenticated(request)
    case "agent" => b2bErrorHandler.onNotAuthenticated(request)
    case _ => webErrorHandler.onNotAuthenticated(request)
  }

  // 403 - Forbidden
  override def onNotAuthorized(implicit request: RequestHeader) = getSubdomain(request) match {
    case "admin" => adminErrorHandler.onNotAuthorized(request)
    case "agent" => b2bErrorHandler.onNotAuthorized(request)
    case _ => webErrorHandler.onNotAuthorized(request)
  }

  // 404 - page not found error
  override def onNotFound(request: RequestHeader, message: String) = getSubdomain(request) match {
    case "admin" => adminErrorHandler.onNotFound(request, message)
    case "agent" => b2bErrorHandler.onNotFound(request, message)
    case "api" => apiErrorHandler.onNotFound(request, message)
    case _ => webErrorHandler.onNotFound(request, message)
  }

  // 500 - internal server error
  override def onProdServerError(request: RequestHeader, exception: UsefulException) = getSubdomain(request) match {
    case "admin" => adminErrorHandler.onProdServerError(request, exception)
    case "agent" => b2bErrorHandler.onProdServerError(request, exception)
    //    case "api" => apiErrorHandler.onProdServerError(request, exception)
    case _ => webErrorHandler.onProdServerError(request, exception)
  }

}

/**
 * The Guice module which wires Error Handler for Silhouette.
 */
import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule

class RootSilhouetteErrorHandlerModule extends AbstractModule with ScalaModule {
  def configure() {
    bind[SecuredErrorHandler].to[ErrorHandler]
    bind[UnsecuredErrorHandler].to[ErrorHandler]
  }
}