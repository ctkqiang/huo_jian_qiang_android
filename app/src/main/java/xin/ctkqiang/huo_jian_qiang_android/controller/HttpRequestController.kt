package xin.ctkqiang.huo_jian_qiang_android.controller

import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import xin.ctkqiang.huo_jian_qiang_android.model.HJQ
import xin.ctkqiang.huo_jian_qiang_android.model.RequestBody
import xin.ctkqiang.huo_jian_qiang_android.model.RequestHeader

@HJQ("HTTP请求控制器")
object HttpRequestController {
    private val client = OkHttpClient()
    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        this.encodeDefaults = true
    }

    suspend fun getRequest(basedUrl: String, requestBody: RequestBody) {
        val requestHeader: MutableList<RequestHeader> = mutableListOf(
            RequestHeader("User-Agent", "Mozilla/5.0"),
            RequestHeader("Content-Type", "application/json"),
            RequestHeader("X-Requested-With", "XMLHttpRequest")
        )

        val url: String = basedUrl + requestBody.toString()

        val request: Request = Request.Builder()
            .url(url)
            .headers(requestHeader.toOkHttpHeaders())
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    "请求失败，错误代码: ${response.code}"
                } else {
                    response.body?.string() ?: "响应体为空"
                }
            }
        } catch (exception: IOException) {
            "网络异常: ${exception.message}"
        }
    }

    suspend fun postRequest(url: String, body: RequestBody) {
        val requestHeader: MutableList<RequestHeader> = mutableListOf(
            RequestHeader("Accept", "*/*"),
            RequestHeader("User-Agent", "Mozilla/5.0"),
            RequestHeader("Accept-Encoding", "gzip, deflate, br"),
            RequestHeader("Connection", "keep-alive"),
            RequestHeader("Content-Type", "application/x-www-form-urlencoded"),
        )

        val jsonContent = jsonConfig.encodeToString(body)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonContent.toRequestBody(mediaType)


        val request: Request = Request.Builder()
            .url(url)
            .headers(requestHeader.toOkHttpHeaders())
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    "请求失败，错误代码: ${response.code}"
                } else {
                    response.body?.string() ?: "响应体为空"
                }
            }
        } catch (exception: IOException) {
            "网络异常: ${exception.message}"
        }
    }

    fun List<RequestHeader>.toOkHttpHeaders(): Headers {
        val builder = Headers.Builder()

        this.forEach { header ->
            builder.add(header.key, header.value)
        }

        return builder.build()
    }
}