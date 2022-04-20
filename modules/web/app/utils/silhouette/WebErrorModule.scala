package utils.silhouette.web

import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.actions.{ SecuredErrorHandler, UnsecuredErrorHandler }
import net.codingwell.scalaguice.ScalaModule

/**
 * Created by Igbalajobi Jamiu Okunade on 6/11/17.
 */

/**
 * The Guice module which wires Error Handler for Silhouette.
 */
class WebSilhouetteErrorHandlerModule extends AbstractModule with ScalaModule {
  def configure() {
    bind[SecuredErrorHandler].to[web.ErrorHandler]
    bind[UnsecuredErrorHandler].to[web.ErrorHandler]
  }
}
