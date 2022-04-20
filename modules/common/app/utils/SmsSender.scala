package utils

import java.io.{ BufferedReader, InputStreamReader, InputStream }
import java.net.{ HttpURLConnection, URL }
import javax.net.ssl.HttpsURLConnection

import dto.HttpResponseDto

/**
 * Created by Igbalajobi Jamiu O. on 11/15/17.
 */
object SmsSender {

  val username = ""
  val password = ""
  val sender = ""

  def sendSms(message: String, title: String, phone: String*) = {
    val stringBuilder = new StringBuilder
    phone.foreach(a => stringBuilder.append(s"$a,"))
    val endpointUrl = s"http://login.betasms.com.ng/api/?username=$username&password=$password&message=$message&sender=$sender&mobiles=${stringBuilder.toString()}"
    get(endpointUrl = endpointUrl, Map())
  }

  var maxTimeoutRetry = 3
  val timeOut = 15 * 1000 //30 Seconds timeout
  def get(endpointUrl: String, headers: Map[String, String]): HttpResponseDto = try {
    val connUrl: URL = new URL(endpointUrl)
    val urlConnection: HttpURLConnection = connUrl.openConnection.asInstanceOf[HttpURLConnection]
    headers.foreach(header => urlConnection.setRequestProperty(header._1, header._2))
    // do post request
    urlConnection.setDoOutput(true)
    urlConnection.setConnectTimeout(timeOut)
    urlConnection.setReadTimeout(timeOut)
    urlConnection.setRequestMethod("GET")

    // read response content
    val inputStream: InputStream = urlConnection.getInputStream
    val resultBuffer: StringBuffer = new StringBuffer
    val inputStreamReader: InputStreamReader = new InputStreamReader(inputStream)
    val reader: BufferedReader = new BufferedReader(inputStreamReader)
    resultBuffer.append(reader.readLine())
    val httpCode: Int = urlConnection.getResponseCode
    //If 'Gateway Timeout' retry the process again
    if (httpCode.equals(504) && maxTimeoutRetry > 0) {
      maxTimeoutRetry -= 1
      return get(endpointUrl = endpointUrl, headers = headers)
    }
    HttpResponseDto(httpCode, resultBuffer.toString, urlConnection.getHeaderFields)
  } catch {
    case e: Exception => e.printStackTrace(); HttpResponseDto(500, "", null)
  }
}
