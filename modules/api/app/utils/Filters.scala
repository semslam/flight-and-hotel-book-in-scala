package api

import play.api.http.HttpFilters
import play.filters.csrf.CSRFFilter
import javax.inject.Inject
import play.filters.gzip.GzipFilter

class Filters @Inject() (gzipFilter: GzipFilter) extends HttpFilters {
  def filters = Seq(gzipFilter)
}