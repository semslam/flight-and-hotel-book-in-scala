package utils.silhouette.b2b

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import com.mohiva.play.silhouette.api.actions.{ SecuredErrorHandler, UnsecuredErrorHandler }

/**
 * The Guice module which wires Error Handler for Silhouette.
 */
class B2BSilhouetteErrorHandlerModule extends AbstractModule with ScalaModule {
  def configure() {
    bind[SecuredErrorHandler].to[b2b.ErrorHandler]
    bind[UnsecuredErrorHandler].to[b2b.ErrorHandler]
  }
}