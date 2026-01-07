package xin.ctkqiang.huo_jian_qiang_android.controller

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import xin.ctkqiang.huo_jian_qiang_android.model.HJQ
import xin.ctkqiang.huo_jian_qiang_android.model.RequestBody
import xin.ctkqiang.huo_jian_qiang_android.model.RequestHeader
import android.util.Log
import java.util.concurrent.TimeUnit

/**
 * HTTP请求结果密封类，模拟Go的(string, int, error)返回类型
 *
 * Go返回类型： (string, int, error)
 * Kotlin适配：使用密封类包装三种可能的结果状态
 *
 * Kotlin特定适配：Go使用多返回值，Kotlin使用密封类实现类型安全的结果处理
 */
sealed class HttpResult {
    /**
     * 请求成功，包含响应体和状态码
     *
     * @property body 响应体字符串
     * @property statusCode HTTP状态码
     */
    data class Success(val body: String, val statusCode: Int) : HttpResult()
    
    /**
     * 请求失败，包含错误信息和可选的HTTP状态码
     *
     * @property message 错误描述信息
     * @property statusCode HTTP状态码（如果有），默认为0表示网络错误
     */
    data class Error(val message: String, val statusCode: Int = 0) : HttpResult()
}

/**
 * 基于Go实现的HTTP请求控制器
 *
 * 功能对齐Go实现：https://raw.githubusercontent.com/ctkqiang/huo_jian_qiang/refs/heads/main/internal/http/http.go
 *
 * Kotlin特定适配：
 * 1. Go的(string, int, error)返回值 → HttpResult密封类
 * 2. Go的错误分类 → Kotlin异常类型映射
 * 3. Go的logger.Infof/Warnf → Android Log类
 * 4. Go的warning.GetIP() → Android网络工具类（目前使用简单日志记录）
 * 5. Go的协程/通道 → Kotlin协程和Flow
 */
@HJQ("HTTP请求控制器 (Go风格实现)")
object HttpRequestController {
    private val TAG = "HttpRequestController"
    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        this.encodeDefaults = true
    }

    /**
     * 执行POST请求 (Go风格实现)
     *
     * @param baseUrl 基础URL地址，必须以http://或https://开头
     * @param body 请求体字符串 (application/x-www-form-urlencoded格式)
     * @param timeout 超时时间（秒），默认30秒
     * @return HttpResult.Success或HttpResult.Error
     *
     * Go对应函数：func PostRequest(basedUrl, body string, timeout int) (string, int, error)
     */
    suspend fun postRequest(baseUrl: String, body: String, timeout: Int = 30): HttpResult {
        // 1. URL格式验证 (与Go实现相同)
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            return HttpResult.Error("URL格式错误：必须以http://或https://开头")
        }

        // 2. 创建HTTP客户端 (与Go实现相同)
        val client = createHTTPClient(timeout)

        // 3. 构建请求 (与Go实现相同)
        val request = try {
            Request.Builder()
                .url(baseUrl)
                .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .apply {
                    // 设置请求头 (与Go实现相同)
                    addHeader("Accept", "*/*")
                    addHeader("User-Agent", "Mozilla/5.0")
                    addHeader("Accept-Encoding", "gzip, deflate, br")
                    addHeader("Connection", "keep-alive")
                    addHeader("Content-Type", "application/x-www-form-urlencoded")
                }
                .build()
        } catch (e: IllegalArgumentException) {
            return HttpResult.Error("创建请求失败: ${e.message}")
        }

        // 4. 日志记录请求信息 (与Go实现相同)
        Log.i(TAG, "发送POST请求: URL=$baseUrl, 内容长度=${body.length}, Content-Type=application/x-www-form-urlencoded")

        // 5. 执行请求并处理响应
        return try {
            client.newCall(request).execute().use { response ->
                val bodyBytes = response.body?.bytes() ?: byteArrayOf()
                val responseBody = bodyBytes.toString(Charsets.UTF_8)
                
                // 处理特定的状态码 (与Go实现相同)
                when (response.code) {
                    504 -> Log.w(TAG, "网关超时 | 状态码: ${response.code}")
                    408 -> Log.w(TAG, "请求超时 | 响应体: $responseBody")
                    400 -> Log.w(TAG, "请求错误 | 状态码: ${response.code}")
                    403 -> Log.w(TAG, "请求被限制 | 状态码: ${response.code}")
                    429 -> Log.w(TAG, "收到[429]状态码 | 请求被限制, 建议增加延迟后重试")
                    200, 201 -> {
                        Log.i(TAG, "请求成功！")
                        Log.i(TAG, "状态码：${response.code}")
                        Log.i(TAG, "响应长度：${bodyBytes.size} 字节")
                        
                        // 响应体预览 (与Go实现相同)
                        if (responseBody.isNotEmpty()) {
                            Log.i(TAG, "    响应体预览：")
                            val previewLength = if (responseBody.length < 300) responseBody.length else 300
                            var preview = responseBody.substring(0, previewLength)
                            preview = preview.replace("\n", "↲ ").replace("\r", "")
                            
                            Log.i(TAG, "   ┌──────────────────────────────────────")
                            Log.i(TAG, "   │ $preview")
                            if (responseBody.length > previewLength) {
                                Log.i(TAG, "   │ ... (还有 ${responseBody.length - previewLength} 个字符)")
                            }
                            Log.i(TAG, "   └──────────────────────────────────────")
                        }
                    }
                    else -> {
                        Log.i(TAG, "   └──────────────────────────────────────")
                    }
                }
                
                // 返回结果 (与Go实现相同：成功时返回响应体、状态码、null错误)
                HttpResult.Success(responseBody, response.code)
            }
        } catch (e: Exception) {
            // 错误分类处理 (与Go实现相同)
            val errorMessage = when {
                e is SocketTimeoutException -> "请求超时"
                e is UnknownHostException -> "不支持的协议方案或链接不存在"
                e is SSLHandshakeException -> "SSL握手失败"
                e is IOException && e.message?.contains("connection reset by peer") == true -> "服务器已关闭"
                e is IOException && e.message?.contains("Canceled") == true -> "请求被取消"
                e is IOException -> "网络异常: ${e.message}"
                else -> "请求异常: ${e.message ?: "未知错误"}"
            }
            
            Log.e(TAG, "POST请求失败: $errorMessage", e)
            HttpResult.Error(errorMessage)
        }
    }

    /**
     * 执行GET请求 (Go风格实现)
     *
     * @param baseUrl 基础URL地址
     * @param paramA 查询参数a的值
     * @param timeout 超时时间（秒），默认30秒
     * @return HttpResult.Success或HttpResult.Error
     *
     * Go对应函数：func GetRequest(baseURL, paramA string, timeout int) (string, int, error)
     */
    private suspend fun getRequestGo(baseUrl: String, paramA: String = "", timeout: Int = 30): HttpResult {
        // 1. 构建完整URL (与Go实现相同)
        val fullUrl = buildURL(baseUrl, paramA).getOrElse { error ->
            return HttpResult.Error(error)
        }

        // 2. 创建HTTP客户端 (与Go实现相同)
        val client = createHTTPClient(timeout)

        // 3. 构建请求 (与Go实现相同)
        val request = try {
            Request.Builder()
                .url(fullUrl)
                .get()
                .apply {
                    // 设置请求头 (与Go实现相同)
                    addHeader("Accept", "*/*")
                    addHeader("User-Agent", "huo_jian_qiang/1.0")
                }
                .build()
        } catch (e: IllegalArgumentException) {
            return HttpResult.Error("创建请求失败: ${e.message}")
        }

        // 4. 执行请求并处理响应
        return try {
            client.newCall(request).execute().use { response ->
                val bodyBytes = response.body?.bytes() ?: byteArrayOf()
                val responseBody = bodyBytes.toString(Charsets.UTF_8)
                
                HttpResult.Success(responseBody, response.code)
            }
        } catch (e: Exception) {
            // 错误分类处理 (与Go实现相同)
            val errorMessage = when {
                e is SocketTimeoutException -> "请求超时"
                e is UnknownHostException -> "不支持的协议方案或链接不存在"
                e is IOException && e.message?.contains("connection reset by peer") == true -> "服务器已关闭"
                e is IOException -> "发送请求失败: ${e.message}"
                else -> "请求异常: ${e.message ?: "未知错误"}"
            }
            
            Log.e(TAG, "GET请求失败: $errorMessage", e)
            HttpResult.Error(errorMessage)
        }
    }

    /**
     * 构建完整URL (Go风格实现)
     *
     * @param baseUrl 基础URL地址
     * @param paramA 查询参数a的值
     * @return 构建成功的完整URL或错误信息
     *
     * Go对应函数：func buildURL(baseURL, paramA string) (string, error)
     */
    private fun buildURL(baseUrl: String, paramA: String): Result<String> {
        // 1. 基础URL验证 (与Go实现相同)
        if (baseUrl.isEmpty()) {
            return Result.failure(IllegalArgumentException("基础URL不能为空"))
        }

        // 2. 如果paramA为空，直接返回baseUrl (与Go实现相同)
        if (paramA.isEmpty()) {
            return Result.success(baseUrl)
        }

        // 3. 构建查询参数 (与Go实现相同)
        val fullUrl = if (baseUrl.contains("?")) {
            "$baseUrl&a=$paramA"
        } else {
            "$baseUrl?a=$paramA"
        }

        // 4. IP解析和日志记录 (模拟Go的warning.GetIP()功能)
        try {
            val url = HttpUrl.parse(fullUrl)
            if (url != null) {
                val host = url.host()
                Log.i(TAG, "地址 $baseUrl: 主机=$host")
                // 注意：Android中获取IP地址需要网络权限和额外处理
                // Go的warning.GetIP()在这里用简单日志替代
            }
        } catch (e: Exception) {
            Log.w(TAG, "解析URL主机失败: ${e.message}")
        }

        return Result.success(fullUrl)
    }

    /**
     * 创建HTTP客户端 (Go风格实现)
     *
     * @param timeout 超时时间（秒）
     * @return 配置好的OkHttpClient实例
     *
     * Go对应函数：func createHTTPClient(timeout int) *http.Client
     */
    private fun createHTTPClient(timeout: Int): OkHttpClient {
        val actualTimeout = if (timeout <= 0) 30 else timeout
        
        return OkHttpClient.Builder()
            .connectTimeout(actualTimeout.toLong(), TimeUnit.SECONDS)
            .readTimeout(actualTimeout.toLong(), TimeUnit.SECONDS)
            .writeTimeout(actualTimeout.toLong(), TimeUnit.SECONDS)
            .build()
    }

    /**
     * =====================================================================
     * 以下为兼容现有Android项目结构的适配函数
     * =====================================================================
     */

    /**
     * 执行GET请求 (兼容现有项目)
     *
     * 将现有的RequestBody参数转换为查询参数，适配Go风格实现
     *
     * @param baseUrl 基础URL地址
     * @param requestBody 请求体参数，包含username和password
     * @return 请求结果字符串，包含状态码和响应内容
     */
    suspend fun getRequest(baseUrl: String, requestBody: RequestBody): String {
        // 构建查询参数字符串
        val paramA = "username=${requestBody.username}&password=${requestBody.password}"
        
        return when (val result = getRequest(baseUrl, paramA)) {
            is HttpResult.Success -> {
                val statusMessage = when (result.statusCode) {
                    200 -> "请求成功 (200): OK"
                    201 -> "请求成功 (201): 已创建"
                    204 -> "请求成功 (204): 无内容"
                    else -> "请求成功，状态码: ${result.statusCode}"
                }
                "$statusMessage\n响应内容: ${result.body}"
            }
            is HttpResult.Error -> {
                val errorPrefix = if (result.statusCode > 0) "HTTP错误" else "网络错误"
                "$errorPrefix: ${result.message}"
            }
        }
    }

    /**
     * 执行POST请求 (兼容现有项目)
     *
     * 将现有的RequestBody参数转换为JSON格式，适配Go风格实现
     *
     * @param url 目标URL地址
     * @param body 请求体参数，包含username和password
     * @return 请求结果字符串，包含状态码和响应内容
     */
    suspend fun postRequest(url: String, body: RequestBody): String {
        // 将RequestBody转换为JSON字符串
        val jsonContent = jsonConfig.encodeToString(body)
        
        return when (val result = postRequest(url, jsonContent)) {
            is HttpResult.Success -> {
                val statusMessage = when (result.statusCode) {
                    200 -> "请求成功 (200): OK"
                    201 -> "请求成功 (201): 已创建"
                    204 -> "请求成功 (204): 无内容"
                    else -> "请求成功，状态码: ${result.statusCode}"
                }
                "$statusMessage\n响应内容: ${result.body}"
            }
            is HttpResult.Error -> {
                val errorPrefix = if (result.statusCode > 0) "HTTP错误" else "网络错误"
                "$errorPrefix: ${result.message}"
            }
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