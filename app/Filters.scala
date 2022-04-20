import play.api.http.HttpFilters
import play.filters.cors.CORSFilter
import play.filters.csrf.CSRFFilter
import javax.inject.Inject

import play.filters.gzip.GzipFilter

class Filters @Inject() (csrfFilter: CSRFFilter, corsFilter: CORSFilter, gzipFilter: GzipFilter) extends HttpFilters {
  def filters = Seq(csrfFilter, corsFilter)
}