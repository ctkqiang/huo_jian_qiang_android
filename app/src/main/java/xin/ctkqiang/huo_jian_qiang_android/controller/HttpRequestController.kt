package xin.ctkqiang.huo_jian_qiang_android.controller

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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

    /**
     * 执行GET请求
     *
     * @param baseUrl 基础URL地址
     * @param requestBody 请求体参数，包含username和password
     * @return 请求结果字符串，包含状态码和响应内容
     * @throws IllegalArgumentException 当URL格式无效时
     */
    suspend fun getRequest(baseUrl: String, requestBody: RequestBody): String {
        val requestHeaders = mutableListOf(
            RequestHeader("User-Agent", "Mozilla/5.0"),
            RequestHeader("Accept", "application/json"),
            RequestHeader("X-Requested-With", "XMLHttpRequest")
        )

        val httpUrl = baseUrl.toHttpUrlOrNull()
        if (httpUrl == null) {
            return "错误: 无效的URL格式 - $baseUrl"
        }

        val url = httpUrl.newBuilder()
            .addQueryParameter("username", requestBody.username)
            .addQueryParameter("password", requestBody.password)
            .build()

        val request = Request.Builder()
            .url(url)
            .headers(requestHeaders.toOkHttpHeaders())
            .get()
            .build()

        return executeRequest(request)
    }

    /**
     * 执行POST请求
     *
     * @param url 目标URL地址
     * @param body 请求体参数，包含username和password
     * @return 请求结果字符串，包含状态码和响应内容
     * @throws IllegalArgumentException 当URL格式无效时
     */
    suspend fun postRequest(url: String, body: RequestBody): String {
        val requestHeaders = mutableListOf(
            RequestHeader("Accept", "*/*"),
            RequestHeader("User-Agent", "Mozilla/5.0"),
            RequestHeader("Accept-Encoding", "gzip, deflate, br"),
            RequestHeader("Connection", "keep-alive"),
            RequestHeader("Content-Type", "application/json"),
        )

        val jsonContent = jsonConfig.encodeToString(body)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonContent.toRequestBody(mediaType)

        val httpUrl = url.toHttpUrlOrNull()
        if (httpUrl == null) {
            return "错误: 无效的URL格式 - $url"
        }

        val request = Request.Builder()
            .url(httpUrl)
            .headers(requestHeaders.toOkHttpHeaders())
            .post(requestBody)
            .build()

        return executeRequest(request)
    }

    /**
     * 执行HTTP请求并处理响应
     *
     * @param request 构建好的OkHttp请求对象
     * @return 格式化后的响应字符串，包含状态码和响应内容
     * @throws IOException 当发生网络异常时
     */
    private fun executeRequest(request: Request): String {
        return try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: "响应体为空"
                
                when {
                    !response.isSuccessful -> {
                        val errorMessage = when (response.code) {
                            400 -> "请求错误 (400): 服务器无法理解请求"
                            401 -> "未授权 (401): 需要身份验证"
                            403 -> "禁止访问 (403): 服务器拒绝请求"
                            404 -> "未找到 (404): 请求的资源不存在"
                            500 -> "服务器错误 (500): 服务器内部错误"
                            else -> "请求失败，错误代码: ${response.code}"
                        }
                        "$errorMessage\n响应内容: $responseBody"
                    }
                    else -> {
                        val successMessage = when (response.code) {
                            200 -> "请求成功 (200): OK"
                            201 -> "请求成功 (201): 已创建"
                            204 -> "请求成功 (204): 无内容"
                            else -> "请求成功，状态码: ${response.code}"
                        }
                        "$successMessage\n响应内容: $responseBody"
                    }
                }
            }
        } catch (exception: IOException) {
            "网络异常: ${exception.message ?: "未知网络错误"}"
        } catch (exception: Exception) {
            "请求异常: ${exception.message ?: "未知错误"}"
        }
    }

    /**
     * 将[RequestHeader]列表转换为OkHttp的Headers对象
     *
     * @return 构建好的Headers对象
     */
    fun List<RequestHeader>.toOkHttpHeaders(): Headers {
        val builder = Headers.Builder()

        this.forEach { header ->
            builder.add(header.key, header.value)
        }

        return builder.build()
    }
}