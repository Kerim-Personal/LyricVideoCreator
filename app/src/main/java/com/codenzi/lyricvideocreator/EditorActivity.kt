package com.codenzi.lyricvideocreator

import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class EditorActivity : AppCompatActivity() {

    //<editor-fold desc="Değişkenler">
    private var songUriString: String? = null
    private var lyricsText: String? = null
    private var mediaPlayer: MediaPlayer? = null

    // Arayüz Elemanları
    private lateinit var playPauseButton: ImageButton
    private lateinit var changeBackgroundButton: FloatingActionButton
    private lateinit var backgroundImageView: ImageView
    private lateinit var seekBar: SeekBar
    private lateinit var exportVideoButton: Button

    // Yapay Zeka Yöneticisi
    private lateinit var whisperManager: WhisperManager

    // SeekBar Güncelleyici
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable
    //</editor-fold>

    //<editor-fold desc="Activity Result API (Dosya Seçiciler)">
    private val selectBackgroundLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            backgroundImageView.setImageURI(uri)
        } else {
            Toast.makeText(this, "Görsel seçilmedi.", Toast.LENGTH_SHORT).show()
        }
    }
    //</editor-fold>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)
        findViewById<android.view.View>(R.id.main)

        // Arayüz elemanlarını bağlama
        bindViews()

        // Gelen verileri alma
        songUriString = intent.getStringExtra("SONG_URI")
        lyricsText = intent.getStringExtra("LYRICS_TEXT")

        // Yapay Zeka Yöneticisini başlatma
        whisperManager = WhisperManager(this)
        loadWhisperModel()

        // Veri kontrolü ve medya oynatıcıyı kurma
        if (songUriString != null && lyricsText != null) {
            Log.d("EditorActivity", "Veriler başarıyla alındı.")
            setupMediaPlayer()
        } else {
            handleDataError()
        }

        // Buton dinleyicilerini ayarlama
        setupClickListeners()
    }

    //<editor-fold desc="Yardımcı Fonksiyonlar">
    private fun bindViews() {
        playPauseButton = findViewById(R.id.playPauseButton)
        changeBackgroundButton = findViewById(R.id.changeBackgroundButton)
        backgroundImageView = findViewById(R.id.backgroundImageView)
        seekBar = findViewById(R.id.seekBar)
        exportVideoButton = findViewById(R.id.exportVideoButton)
    }

    private fun handleDataError() {
        Log.e("EditorActivity", "Gerekli veriler (URI veya Şarkı Sözü) alınamadı!")
        Toast.makeText(this, "Hata: Şarkı bilgileri alınamadı.", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun loadWhisperModel() {
        lifecycleScope.launch {
            val success = whisperManager.loadModel()
            if (success) {
                Log.d("EditorActivity", "Whisper modeli başarıyla yüklendi.")
                Toast.makeText(this@EditorActivity, "AI Modeli Hazır", Toast.LENGTH_SHORT).show()
            } else {
                Log.e("EditorActivity", "Whisper modeli yüklenemedi!")
                Toast.makeText(this@EditorActivity, "AI Modeli Yüklenemedi", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupClickListeners() {
        playPauseButton.setOnClickListener {
            togglePlayPause()
        }

        changeBackgroundButton.setOnClickListener {
            selectBackgroundLauncher.launch("image/*")
        }

        exportVideoButton.setOnClickListener {
            Toast.makeText(this, "Senkronizasyon başlatılıyor...", Toast.LENGTH_SHORT).show()
            songUriString?.let { uriString ->
                lifecycleScope.launch {
                    val result = whisperManager.transcribe(Uri.parse(uriString))
                    if (result != null) {
                        Log.d("EditorActivity", "İŞTE SONUÇ:\n$result")
                        Toast.makeText(this@EditorActivity, "Senkronizasyon tamamlandı!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@EditorActivity, "Senkronizasyon başarısız.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupMediaPlayer() {
        mediaPlayer = MediaPlayer()
        try {
            mediaPlayer?.setDataSource(this, Uri.parse(songUriString))
            mediaPlayer?.prepareAsync()
            mediaPlayer?.setOnPreparedListener { player ->
                seekBar.max = player.duration
                player.start()
                playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
                startSeekBarUpdate()
            }
            mediaPlayer?.setOnCompletionListener {
                playPauseButton.setImageResource(android.R.drawable.ic_media_play)
                handler.removeCallbacks(runnable)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Hata: Medya oynatıcısı kurulamadı.", Toast.LENGTH_LONG).show()
        }
    }

    private fun startSeekBarUpdate() {
        runnable = Runnable {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    seekBar.progress = it.currentPosition
                    handler.postDelayed(runnable, 500)
                }
            }
        }
        handler.post(runnable)
    }

    private fun togglePlayPause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                playPauseButton.setImageResource(android.R.drawable.ic_media_play)
                handler.removeCallbacks(runnable)
            } else {
                player.start()
                playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
                startSeekBarUpdate()
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="Lifecycle Metodu">
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacks(runnable)
    }
    //</editor-fold>
}