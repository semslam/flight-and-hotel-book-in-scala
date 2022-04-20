import java.util.regex.Pattern
import javax.inject.Inject

import play.api.http._
import play.api.mvc._
import play.api.routing.Router

class VirtualHostRequestHandler @Inject() (
    errorHandler: HttpErrorHandler,
    configuration: HttpConfiguration,
    apiConfig: play.api.Configuration,
    filters: HttpFilters,
    webRouter: web.Routes,
    backendRouter: admin.Routes,
    b2bRouter: b2b.Routes,
    apiRouter: api.Routes
) extends DefaultHttpRequestHandler(webRouter, errorHandler, configuration, filters) {

  override def routeRequest(request: RequestHeader) = getSubdomain(request) match {
    case "admin" => backendRouter.routes.lift(rewriteAssets("admin", request))
    case "agent" => b2bRouter.routes.lift(rewriteAssets("agent", request))
    case "api" => apiRouter.routes.lift(rewriteAssets("api", request))
    case _ => webRouter.routes.lift(rewriteAssets("web", request))
  }

  /*
	* Gets the subdomain: "admin" o "www"
	*/
  private def getSubdomain(request: RequestHeader) = request.domain.replaceFirst("[\\.]?[^\\.]+[\\.][^\\.]+$", "")

  /*
	* Rewrite the Assets routes for the root project, accessing to the corresponding lib.
	*/
  private def rewriteAssets(subproject: String, request: RequestHeader): RequestHeader = {
    val pub = s"""/public/(.*)""".r
    val css = s"""/css/(.*)""".r
    val js = s"""/js/(.*)""".r
    val img = s"""/img/(.*)""".r
    request.path match {
      case pub(file) => request.copy(path = s"/lib/$subproject/$file")
      case css(file) => request.copy(path = s"/lib/$subproject/stylesheets/$file")
      case js(file) => request.copy(path = s"/lib/$subproject/javascripts/$file")
      case img(file) => request.copy(path = s"/lib/$subproject/images/$file")
      case _ => request
    }
  }

  override def handlerForRequest(request: RequestHeader): (RequestHeader, Handler) = getSubdomain(request).toLowerCase match {
    case _ => super.handlerForRequest(request)
  }

}
