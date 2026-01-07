package xin.ctkqiang.huo_jian_qiang_android.controller

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.charset.Charset

import android.os.Environment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * 文件下载器，提供文件下载和存在性检查功能
 *
 * 使用OkHttp进行网络请求，支持协程异步操作
 */
object Downloader {
    private const val TAG = "Downloader"
    private const val DEFAULT_FILENAME = "rockyou.txt"
    private const val NOTIFICATION_CHANNEL_ID = "download_channel"
    private const val NOTIFICATION_ID = 1

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "文件下载"
            val descriptionText = "显示文件下载进度"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 获取文件存储目录
     * 优先使用外部存储的 Download 目录，以便用户可见
     */
    private fun getFileDir(context: Context): File {
        // 尝试获取外部公共 Download 目录
        val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val appDir = File(publicDir, "HuoJianQiang")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        return appDir
    }

    /**
     * 检查指定文件是否存在
     *
     * @param context 应用上下文
     * @param filename 要检查的文件名，默认为 "rockyou.txt"
     * @return 如果文件存在则返回true，否则返回false
     */
    fun isFileExists(context: Context, filename: String = DEFAULT_FILENAME): Boolean {
        return try {
            val file = File(getFileDir(context), filename)
            file.exists() && file.length() > 0
        } catch (e: SecurityException) {
            Log.w(TAG, "权限检查失败: ${e.message}")
            false
        }
    }

    /**
     * 下载文件到应用的文件目录
     *
     * @param context 应用上下文
     * @param url 文件下载URL
     * @param filename 保存的文件名默，认为 "rockyou.txt"
     * @param maxRetries 最大重试次数，默认为3次
     * @return 如果下载成功则返回true，否则返回false
     */
    suspend fun downloadFile(
        context: Context,
        url: String,
        filename: String = DEFAULT_FILENAME,
        maxRetries: Int = 3
    ): Boolean = withContext(Dispatchers.IO) {
        require(url.isNotBlank()) { "URL不能为空" }
        require(filename.isNotBlank()) { "文件名不能为空" }
        
        var retryCount = 0
        var lastException: Exception? = null
        
        while (retryCount < maxRetries) {
            try {
                return@withContext executeDownload(context, url, filename).also {
                    if (it) {
                        Log.i(TAG, "文件下载成功: $filename")
                    }
                }
            } catch (e: Exception) {
                lastException = e
                retryCount++
                
                when (e) {
                    is SocketTimeoutException -> {
                        Log.w(TAG, "下载超时，尝试第${retryCount}次重试: ${e.message}")
                    }
                    is UnknownHostException -> {
                        Log.w(TAG, "网络不可用，尝试第${retryCount}次重试: ${e.message}")
                    }
                    is IOException -> {
                        Log.w(TAG, "IO异常，尝试第${retryCount}次重试: ${e.message}")
                    }
                    else -> {
                        Log.e(TAG, "下载失败，异常类型: ${e.javaClass.simpleName}, 消息: ${e.message}")
                        return@withContext false
                    }
                }
                
                if (retryCount < maxRetries) {
                    kotlinx.coroutines.delay(1000L * retryCount)
                }
            }
        }
        
        Log.e(TAG, "下载失败，已达到最大重试次数: $maxRetries")
        lastException?.printStackTrace()
        false
    }

    /**
     * 执行具体的下载操作
     *
     * @param context 应用上下文
     * @param url 文件下载URL
     * @param filename 保存的文件名
     * @return 如果下载成功则返回true，否则返回false
     * @throws IOException 当发生网络或IO异常时
     */
    private fun executeDownload(context: Context, url: String, filename: String): Boolean {
        createNotificationChannel(context)
        val notificationManager = NotificationManagerCompat.from(context)
        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("正在下载 $filename")
            .setContentText("下载准备中...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setProgress(100, 0, true)

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || 
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context, 
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(NOTIFICATION_ID, builder.build())
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "无法显示通知: 权限被拒绝")
        }

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .header("Accept", "*/*")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "HTTP请求失败: ${response.code} - ${response.message}")
                return false
            }

            val dir = getFileDir(context)
            val file = File(dir, filename)
            val tempFile = File(dir, "$filename.tmp")

            val contentLength = response.body?.contentLength() ?: -1L
            var downloadedBytes = 0L

            response.body?.use { body ->
                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var lastProgressUpdate = 0L
                        
                        // 将不定长进度改为定长（如果已知）
                        if (contentLength > 0) {
                            builder.setProgress(100, 0, false)
                        }

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            
                            // 更新进度条（每500ms更新一次，避免太频繁）
                            val currentTime = System.currentTimeMillis()
                            if (contentLength > 0 && currentTime - lastProgressUpdate > 500) {
                                val progress = ((downloadedBytes * 100) / contentLength).toInt()
                                builder.setProgress(100, progress, false)
                                    .setContentText("下载中: $progress%")
                                try {
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || 
                                        androidx.core.content.ContextCompat.checkSelfPermission(
                                            context, 
                                            android.Manifest.permission.POST_NOTIFICATIONS
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                        notificationManager.notify(NOTIFICATION_ID, builder.build())
                                    }
                                } catch (e: Exception) {}
                                lastProgressUpdate = currentTime
                            }
                        }
                    }
                }
            } ?: run {
                Log.w(TAG, "响应体为空")
                return false
            }

            if (tempFile.length() == 0L) {
                Log.w(TAG, "下载的文件大小为0")
                tempFile.delete()
                return false
            }

            if (file.exists()) {
                file.delete()
            }

            val success = if (tempFile.renameTo(file)) {
                Log.d(TAG, "文件下载完成: ${file.absolutePath}, 大小: ${file.length()} 字节")
                true
            } else {
                Log.w(TAG, "文件重命名失败")
                // 尝试直接复制（处理某些文件系统跨分区移动问题）
                try {
                    tempFile.copyTo(file, overwrite = true)
                    tempFile.delete()
                    Log.d(TAG, "文件复制完成 (fallback): ${file.absolutePath}")
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "文件复制失败: ${e.message}")
                    false
                }
            }

            // 下载完成后移除通知
            try {
                if (success) {
                    builder.setContentText("下载完成")
                        .setProgress(0, 0, false)
                        .setOngoing(false)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                } else {
                    builder.setContentText("下载失败")
                        .setProgress(0, 0, false)
                        .setOngoing(false)
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || 
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context, 
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(NOTIFICATION_ID, builder.build())
                }
                
                // 延迟取消通知
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
                    delay(3000)
                    notificationManager.cancel(NOTIFICATION_ID)
                }
            } catch (e: Exception) {}

            return success
        }
    }

    /**
     * 向后兼容的方法：下载rockyou字典文件
     *
     * @param context 应用上下文
     * @param url 文件下载URL
     * @return 如果下载成功则返回true，否则返回false
     * @deprecated 使用 {@link #downloadFile} 代替
     */
    @Deprecated(
        "使用 downloadFile 代替",
        ReplaceWith("downloadFile(context, url, DEFAULT_FILENAME)")
    )
    suspend fun downloadRockYou(context: Context, url: String): Boolean {
        return downloadFile(context, url, DEFAULT_FILENAME)
    }

    /**
     * 删除指定的文件
     *
     * @param context 应用上下文
     * @param filename 要删除的文件名，默认为 "rockyou.txt"
     * @return 如果删除成功或文件不存在则返回true，否则返回false
     */
    fun deleteFile(context: Context, filename: String = DEFAULT_FILENAME): Boolean {
        return try {
            val file = File(getFileDir(context), filename)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "删除文件失败: ${e.message}")
            false
        }
    }

    /**
     * 获取文件大小
     *
     * @param context 应用上下文
     * @param filename 要检查的文件名，默认为 "rockyou.txt"
     * @return 文件大小（字节），如果文件不存在则返回0
     */
    fun getFileSize(context: Context, filename: String = DEFAULT_FILENAME): Long {
        return try {
            val file = File(getFileDir(context), filename)
            if (file.exists()) file.length() else 0
        } catch (e: SecurityException) {
            Log.w(TAG, "获取文件大小失败: ${e.message}")
            0
        }
    }

    /**
     * 检查文件是否可读
     *
     * @param context 应用上下文
     * @param filename 要检查的文件名，默认为 "rockyou.txt"
     * @return 如果文件存在且可读则返回true，否则返回false
     */
    fun isFileReadable(context: Context, filename: String = DEFAULT_FILENAME): Boolean {
        return try {
            val file = File(getFileDir(context), filename)
            file.exists() && file.canRead() && file.length() > 0
        } catch (e: SecurityException) {
            Log.w(TAG, "文件可读性检查失败: ${e.message}")
            false
        }
    }

    /**
     * 读取文件内容为字符串
     *
     * @param context 应用上下文
     * @param filename 要读取的文件名，默认为 "rockyou.txt"
     * @param charset 字符编码，默认为 UTF-8
     * @return 文件内容字符串，如果读取失败则返回空字符串
     */
    fun readFileAsText(
        context: Context,
        filename: String = DEFAULT_FILENAME,
        charset: Charset = Charsets.UTF_8
    ): String {
        return try {
            val file = File(getFileDir(context), filename)
            if (!file.exists() || !file.canRead()) {
                Log.w(TAG, "文件不存在或不可读: $filename")
                return ""
            }
            
            file.readText(charset)
        } catch (e: IOException) {
            Log.e(TAG, "读取文件失败 (IO异常): ${e.message}")
            ""
        } catch (e: SecurityException) {
            Log.e(TAG, "读取文件失败 (权限异常): ${e.message}")
            ""
        } catch (e: Exception) {
            Log.e(TAG, "读取文件失败 (未知异常): ${e.message}")
            ""
        }
    }

    /**
     * 读取文件内容为字节数组
     *
     * @param context 应用上下文
     * @param filename 要读取的文件名，默认为 "rockyou.txt"
     * @return 文件内容字节数组，如果读取失败则返回空字节数组
     */
    fun readFileAsBytes(context: Context, filename: String = DEFAULT_FILENAME): ByteArray {
        return try {
            val file = File(getFileDir(context), filename)
            if (!file.exists() || !file.canRead()) {
                Log.w(TAG, "文件不存在或不可读: $filename")
                return ByteArray(0)
            }
            
            file.readBytes()
        } catch (e: IOException) {
            Log.e(TAG, "读取文件字节失败 (IO异常): ${e.message}")
            ByteArray(0)
        } catch (e: SecurityException) {
            Log.e(TAG, "读取文件字节失败 (权限异常): ${e.message}")
            ByteArray(0)
        } catch (e: Exception) {
            Log.e(TAG, "读取文件字节失败 (未知异常): ${e.message}")
            ByteArray(0)
        }
    }

    /**
     * 逐行读取文件内容
     *
     * @param context 应用上下文
     * @param filename 要读取的文件名，默认为 "rockyou.txt"
     * @param charset 字符编码，默认为 UTF-8
     * @param skipEmptyLines 是否跳过空行，默认为true
     * @param limit 最大读取行数限制，默认为0表示无限制
     * @return 文件行列表，如果读取失败则返回空列表
     */
    fun readFileAsLines(
        context: Context,
        filename: String = DEFAULT_FILENAME,
        charset: Charset = Charsets.UTF_8,
        skipEmptyLines: Boolean = true,
        limit: Int = 0
    ): List<String> {
        return try {
            val file = File(getFileDir(context), filename)
            if (!file.exists() || !file.canRead()) {
                Log.w(TAG, "文件不存在或不可读: $filename")
                return emptyList()
            }

            val lines = mutableListOf<String>()
            var lineCount = 0
            
            file.bufferedReader(charset).use { reader ->
                reader.forEachLine { line ->
                    if (limit > 0 && lineCount >= limit) {
                        return@forEachLine
                    }
                    
                    if (!skipEmptyLines || line.isNotBlank()) {
                        lines.add(line)
                        lineCount++
                    }
                }
            }
            
            lines
        } catch (e: IOException) {
            Log.e(TAG, "逐行读取文件失败 (IO异常): ${e.message}")
            emptyList()
        } catch (e: SecurityException) {
            Log.e(TAG, "逐行读取文件失败 (权限异常): ${e.message}")
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "逐行读取文件失败 (未知异常): ${e.message}")
            emptyList()
        }
    }

    /**
     * 获取文件路径
     *
     * @param context 应用上下文
     * @param filename 要获取路径的文件名，默认为 "rockyou.txt"
     * @return 文件的完整路径，如果文件不存在则返回空字符串
     */
    fun getFilePath(context: Context, filename: String = DEFAULT_FILENAME): String {
        return try {
            val file = File(getFileDir(context), filename)
            file.absolutePath
        } catch (e: SecurityException) {
            Log.w(TAG, "获取文件路径失败: ${e.message}")
            ""
        }
    }

    /**
     * 验证文件编码是否为UTF-8
     *
     * @param context 应用上下文
     * @param filename 要验证的文件名，默认为 "rockyou.txt"
     * @return 如果文件编码可能是UTF-8则返回true，否则返回false
     */
    fun isFileEncodingValid(context: Context, filename: String = DEFAULT_FILENAME): Boolean {
        return try {
            val file = File(getFileDir(context), filename)
            if (!file.exists() || !file.canRead()) {
                Log.w(TAG, "文件不存在或不可读: $filename")
                return false
            }

            // 读取文件前几个字节来检查UTF-8 BOM或可打印字符
            file.inputStream().use { input ->
                val buffer = ByteArray(1024)
                val bytesRead = input.read(buffer)
                if (bytesRead <= 0) {
                    return true // 空文件视为有效
                }

                // 简单UTF-8检查：检查是否有BOM或可打印ASCII/UTF-8字符
                val sample = String(buffer, 0, bytesRead, Charsets.UTF_8)
                val isValid = sample.toCharArray().all { char ->
                    char.code in 0..0x10FFFF && !char.isSurrogate()
                }
                
                if (!isValid) {
                    Log.w(TAG, "文件可能不是有效的UTF-8编码: $filename")
                }
                
                isValid
            }
        } catch (e: Exception) {
            Log.e(TAG, "验证文件编码失败: ${e.message}")
            false
        }
    }

    /**
     * 高效逐行处理文件内容
     *
     * 此方法使用缓冲读取器逐行处理大文件，避免一次性加载整个文件到内存
     *
     * @param context 应用上下文
     * @param filename 要处理的文件名，默认为 "rockyou.txt"
     * @param onLine 每读取一行时调用的回调函数，接收行号（从1开始）和行内容
     * @param charset 字符编码，默认为 UTF-8
     * @param skipEmptyLines 是否跳过空行，默认为true
     * @param limit 最大处理行数限制，默认为0表示无限制
     * @return 成功处理的行数，如果处理失败则返回-1
     */
    fun processFileByLine(
        context: Context,
        filename: String = DEFAULT_FILENAME,
        onLine: (lineNumber: Int, line: String) -> Unit,
        charset: Charset = Charsets.UTF_8,
        skipEmptyLines: Boolean = true,
        limit: Int = 0
    ): Int {
        return try {
            val file = File(getFileDir(context), filename)
            if (!file.exists() || !file.canRead()) {
                Log.w(TAG, "文件不存在或不可读: $filename")
                return -1
            }

            var lineCount = 0
            var processedCount = 0
            
            file.bufferedReader(charset).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    lineCount++
                    
                    if (limit > 0 && processedCount >= limit) {
                        break
                    }
                    
                    val trimmedLine = line!!.trim()
                    if (skipEmptyLines && trimmedLine.isEmpty()) {
                        continue
                    }
                    
                    onLine(lineCount, trimmedLine)
                    processedCount++
                }
            }
            
            Log.d(TAG, "文件处理完成: $filename, 总行数: $lineCount, 处理行数: $processedCount")
            processedCount
        } catch (e: IOException) {
            Log.e(TAG, "逐行处理文件失败 (IO异常): ${e.message}")
            -1
        } catch (e: SecurityException) {
            Log.e(TAG, "逐行处理文件失败 (权限异常): ${e.message}")
            -1
        } catch (e: Exception) {
            Log.e(TAG, "逐行处理文件失败 (未知异常): ${e.message}")
            -1
        }
    }

    /**
     * 处理密码文件（rockyou.txt）并生成密码列表
     *
     * 此方法专门用于处理密码字典文件，自动验证文件并处理特殊字符
     *
     * @param context 应用上下文
     * @param filename 密码文件名，默认为 "rockyou.txt"
     * @param onPassword 每读取一个密码时调用的回调函数，接收行号和密码内容
     * @param limit 最大处理密码数量限制，默认为0表示无限制
     * @return 成功处理的密码数量，如果处理失败则返回-1
     */
    fun processPasswordFile(
        context: Context,
        filename: String = DEFAULT_FILENAME,
        onPassword: (lineNumber: Int, password: String) -> Unit,
        limit: Int = 0
    ): Int {
        if (!isFileExists(context, filename)) {
            Log.w(TAG, "密码文件不存在: $filename")
            return -1
        }
        
        if (!isFileReadable(context, filename)) {
            Log.w(TAG, "密码文件不可读: $filename")
            return -1
        }
        
        if (!isFileEncodingValid(context, filename)) {
            Log.w(TAG, "密码文件编码可能无效: $filename")
            // 继续处理，但记录警告
        }
        
        return processFileByLine(
            context = context,
            filename = filename,
            onLine = { lineNumber, line ->
                // 对于密码文件，我们进行额外的验证和清理
                val password = line.trim()
                if (password.isNotEmpty()) {
                    onPassword(lineNumber, password)
                }
            },
            charset = Charsets.UTF_8,
            skipEmptyLines = true,
            limit = limit
        )
    }

    /**
     * 获取密码文件中的密码数量
     *
     * @param context 应用上下文
     * @param filename 密码文件名，默认为 "rockyou.txt"
     * @return 密码数量，如果无法读取则返回-1
     */
    fun getPasswordCount(context: Context, filename: String = DEFAULT_FILENAME): Int {
        return try {
            val file = File(getFileDir(context), filename)
            if (!file.exists() || !file.canRead()) {
                return -1
            }
            
            var count = 0
            file.bufferedReader().use { reader ->
                while (reader.readLine() != null) {
                    count++
                }
            }
            count
        } catch (e: Exception) {
            Log.e(TAG, "获取密码数量失败: ${e.message}")
            -1
        }
    }

    /**
     * 从密码文件中获取指定范围的密码
     *
     * @param context 应用上下文
     * @param startLine 起始行号（从1开始）
     * @param count 要获取的密码数量
     * @param filename 密码文件名，默认为 "rockyou.txt"
     * @return 密码列表，如果获取失败则返回空列表
     */
    fun getPasswordsInRange(
        context: Context,
        startLine: Int,
        count: Int,
        filename: String = DEFAULT_FILENAME
    ): List<String> {
        return try {
            val file = File(getFileDir(context), filename)
            if (!file.exists() || !file.canRead()) {
                return emptyList()
            }
            
            val passwords = mutableListOf<String>()
            var currentLine = 0
            
            file.bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    currentLine++
                    
                    if (currentLine >= startLine && currentLine < startLine + count) {
                        val password = line!!.trim()
                        if (password.isNotEmpty()) {
                            passwords.add(password)
                        }
                    }
                    
                    if (currentLine >= startLine + count) {
                        break
                    }
                }
            }
            
            passwords
        } catch (e: Exception) {
            Log.e(TAG, "获取指定范围密码失败: ${e.message}")
            emptyList()
        }
    }
}