package xin.ctkqiang.huo_jian_qiang_android.controller

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class Downloader {
    companion object {
        private val client = OkHttpClient()

        fun isFileExists(context: Context): Boolean {
            return File(context.filesDir, "rockyou.txt").exists()
        }

        suspend fun downloadRockYou(context: Context, url: String): Boolean = withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()

            return@withContext try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext false

                    val file = File(context.filesDir, "rockyou.txt")

                    response.body?.byteStream()?.use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                    true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}