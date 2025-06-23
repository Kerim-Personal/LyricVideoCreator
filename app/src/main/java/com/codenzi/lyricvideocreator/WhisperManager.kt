package com.codenzi.lyricvideocreator

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class WhisperManager(private val context: Context) {

    // Bu blok, C++ kütüphanemizi uygulamaya yükler.
    companion object {
        init {
            Log.d("WhisperManager", "Whisper JNI kütüphanesi yükleniyor...")
            System.loadLibrary("whisper-jni")
        }
    }

    // Bu fonksiyonlar Kotlin'den C++'a açılan kapılardır.
    // "external" anahtar kelimesi, bu fonksiyonların gövdesinin C++'da yazıldığını belirtir.
    private external fun loadModelNative(modelPath: String): Boolean
    private external fun transcribeFileNative(filePath: String): String

    /**
     * Whisper modelini assets klasöründen telefonun hafızasına kopyalar ve C++ tarafında yükler.
     */
    suspend fun loadModel(modelName: String = "models/ggml-tiny.en.bin") = withContext(Dispatchers.IO) {
        try {
            val modelFile = File(context.filesDir, modelName)
            if (!modelFile.exists()) {
                Log.d("WhisperManager", "Model dosyası bulunamadı, assets'ten kopyalanıyor...")
                val inputStream: InputStream = context.assets.open(modelName)
                val outputStream = FileOutputStream(modelFile)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()
                Log.d("WhisperManager", "Model başarıyla kopyalandı: ${modelFile.absolutePath}")
            } else {
                Log.d("WhisperManager", "Model dosyası zaten mevcut.")
            }

            loadModelNative(modelFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Verilen ses dosyasını C++ tarafına gönderip senkronize edilmiş metni geri alır.
     */
    suspend fun transcribe(audioUri: Uri): String? = withContext(Dispatchers.IO) {
        Log.d("WhisperManager", "Transkripsiyon işlemi başlatılıyor...")
        try {
            // Content URI'sini gerçek bir dosya yoluna çevirmemiz gerekebilir,
            // bu adımı şimdilik basitleştiriyoruz.
            // Önce dosyayı geçici bir konuma kopyalayıp yolunu alıyoruz.
            val tempAudioFile = copyUriToTempFile(audioUri)
            if (tempAudioFile != null) {
                val result = transcribeFileNative(tempAudioFile.absolutePath)
                tempAudioFile.delete() // Geçici dosyayı sil
                Log.d("WhisperManager", "Transkripsiyon tamamlandı.")
                result
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // URI'dan gelen veriyi geçici bir dosyaya kopyalayan yardımcı fonksiyon
    private fun copyUriToTempFile(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("audio", ".wav", context.cacheDir)
            tempFile.deleteOnExit()
            val outputStream = FileOutputStream(tempFile)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}