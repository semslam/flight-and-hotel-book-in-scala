package controllers.b2b

import play.api.Configuration
import javax.inject.Inject

class Assets @Inject() (val errorHandler: b2b.ErrorHandler) extends controllers.common.Assets(errorHandler)
class SharedResources @Inject() (val errorHandler: b2b.ErrorHandler, val conf: Configuration) extends controllers.common.SharedResources(errorHandler, conf)