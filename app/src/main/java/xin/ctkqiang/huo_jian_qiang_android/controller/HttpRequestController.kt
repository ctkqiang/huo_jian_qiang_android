package xin.ctkqiang.huo_jian_qiang_android.controller

import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import xin.ctkqiang.huo_jian_qiang_android.model.HJQ
import xin.ctkqiang.huo_jian_qiang_android.model.RequestBody
import xin.ctkqiang.huo_jian_qiang_android.model.RequestHeader

@HJQ("HTTP请求控制器")
object HttpRequestController {
    private val client = OkHttpClient()

    suspend fun getRequest(url:String,  requestBody: RequestBody) {
        val requestHeader : MutableList<RequestHeader> = mutableListOf(
            RequestHeader("User-Agent", "Mozilla/5.0"),
            RequestHeader("Content-Type", "application/json"),
            RequestHeader("X-Requested-With", "XMLHttpRequest")
        )

        val request : Request = Request.Builder()
            .url(url)
            .headers(requestHeader.toOkHttpHeaders())
            .build()
    }

    fun List<RequestHeader>.toOkHttpHeaders(): Headers {
        val builder = Headers.Builder()

        this.forEach { header ->
            builder.add(header.key, header.value)
        }

        return builder.build()
    }
}