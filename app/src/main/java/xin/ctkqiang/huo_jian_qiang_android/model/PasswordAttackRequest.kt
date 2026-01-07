package xin.ctkqiang.huo_jian_qiang_android.model

import androidx.annotation.Keep

/**
 * 密码攻击请求配置
 *
 * @param targetUrl 目标URL地址
 * @param passwordFile 密码文件名称，默认为 rockyou.txt
 * @param startLine 起始行号（1-based），默认为1
 * @param endLine 结束行号（包含），默认为null（处理整个文件）
 * @param requestsPerSecond 每秒请求数，默认为5
 * @param connectTimeoutSeconds 连接超时时间（秒），默认为30
 * @param readTimeoutSeconds 读取超时时间（秒），默认为60
 * @param writeTimeoutSeconds 写入超时时间（秒），默认为30
 * @param maxRetries 最大重试次数，默认为3
 * @param retryDelayMs 重试延迟时间（毫秒），默认为1000
 */
@Keep
data class PasswordAttackRequest(
    val targetUrl: String,
    val passwordFile: String = "rockyou.txt",
    val startLine: Int = 1,
    val endLine: Int? = null,
    val requestsPerSecond: Int = 5,
    val connectTimeoutSeconds: Long = 30,
    val readTimeoutSeconds: Long = 60,
    val writeTimeoutSeconds: Long = 30,
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 1000
) {
    init {
        require(requestsPerSecond > 0) { "requestsPerSecond 必须大于0" }
        require(startLine > 0) { "startLine 必须大于0" }
        if (endLine != null) {
            require(endLine >= startLine) { "endLine 必须大于等于 startLine" }
        }
        require(connectTimeoutSeconds > 0) { "connectTimeoutSeconds 必须大于0" }
        require(readTimeoutSeconds > 0) { "readTimeoutSeconds 必须大于0" }
        require(writeTimeoutSeconds > 0) { "writeTimeoutSeconds 必须大于0" }
        require(maxRetries >= 0) { "maxRetries 必须大于等于0" }
        require(retryDelayMs >= 0) { "retryDelayMs 必须大于等于0" }
    }
}

/**
 * 密码攻击结果
 *
 * @param lineNumber 行号（1-based）
 * @param password 密码字符串
 * @param success 是否请求成功
 * @param response 响应内容
 * @param statusCode HTTP状态码
 * @param elapsedTime 请求耗时（毫秒）
 * @param attempts 尝试次数
 * @param errorMessage 错误信息（如果有）
 */
@Keep
data class PasswordAttackResult(
    val lineNumber: Int,
    val password: String,
    val success: Boolean,
    val response: String? = null,
    val statusCode: Int? = null,
    val elapsedTime: Long = 0,
    val attempts: Int = 1,
    val errorMessage: String? = null
)

/**
 * 密码攻击统计信息
 *
 * @param totalLines 总行数
 * @param processedLines 已处理行数
 * @param successfulRequests 成功请求数
 * @param failedRequests 失败请求数
 * @param totalRequests 总请求数
 * @param totalTime 总耗时（毫秒）
 * @param averageTime 平均请求耗时（毫秒）
 */
@Keep
data class PasswordAttackStats(
    val totalLines: Int = 0,
    val processedLines: Int = 0,
    val successfulRequests: Int = 0,
    val failedRequests: Int = 0,
    val totalRequests: Int = 0,
    val totalTime: Long = 0,
    val averageTime: Long = 0
) {
    val progress: Float
        get() = if (totalLines > 0) processedLines.toFloat() / totalLines else 0f
    
    val successRate: Float
        get() = if (totalRequests > 0) successfulRequests.toFloat() / totalRequests else 0f
}