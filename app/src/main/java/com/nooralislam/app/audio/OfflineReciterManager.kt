package com.nooralislam.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.nooralislam.app.data.ReciterInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class OfflineReciterManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    private fun getSurahFile(reciterId: String, surahId: Int): File {
        val dir = File(context.filesDir, "reciters/$reciterId")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "${surahId.toString().padStart(3, '0')}.mp3")
    }

    fun isSurahDownloaded(reciterId: String, surahId: Int): Boolean {
        val f = getSurahFile(reciterId, surahId)
        return f.exists() && f.length() > 1024
    }

    fun countDownloadedForReciter(reciterId: String): Int {
        val dir = File(context.filesDir, "reciters/$reciterId")
        return dir.listFiles()?.count { it.extension == "mp3" && it.length() > 1024 } ?: 0
    }

    suspend fun downloadSurah(reciter: ReciterInfo, surahId: Int): Boolean = withContext(Dispatchers.IO) {
        val target = getSurahFile(reciter.id, surahId)
        if (target.exists() && target.length() > 1024) return@withContext true
        val tmp = File(target.parentFile, "${target.name}.tmp")
        try {
            val conn = URL(reciter.buildSurahUrl(surahId)).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.instanceFollowRedirects = true
            conn.connect()
            if (conn.responseCode !in 200..299) return@withContext false
            conn.inputStream.use { input ->
                tmp.outputStream().use { output -> input.copyTo(output) }
            }
            tmp.renameTo(target)
            true
        } catch (_: Exception) {
            tmp.delete()
            false
        }
    }

    fun playSurah(reciter: ReciterInfo, surahId: Int, onPrepared: () -> Unit = {}, onError: (String) -> Unit = {}) {
        stop()
        val localFile = getSurahFile(reciter.id, surahId)
        val source = if (localFile.exists() && localFile.length() > 1024) {
            localFile.absolutePath
        } else {
            reciter.buildSurahUrl(surahId)
        }
        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(source)
                setOnPreparedListener {
                    it.start()
                    onPrepared()
                }
                setOnErrorListener { _, _, _ ->
                    onError("تعذر تشغيل التلاوة — تأكد من الاتصال أو نزّل السورة للاستماع بدون نت")
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (e: Exception) {
            onError(e.message ?: "تعذر تشغيل الصوت")
        }
    }

    fun stop() {
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }
}
