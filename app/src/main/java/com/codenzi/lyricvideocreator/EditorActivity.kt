package com.codenzi.lyricvideocreator

import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.floatingactionbutton.FloatingActionButton

class EditorActivity : AppCompatActivity() {

    private var songUriString: String? = null
    private var lyricsText: String? = null
    private var mediaPlayer: MediaPlayer? = null

    private lateinit var playPauseButton: ImageButton
    private lateinit var changeBackgroundButton: FloatingActionButton
    private lateinit var backgroundImageView: ImageView
    private lateinit var seekBar: SeekBar // SeekBar'ı class seviyesinde tanımla

    // YENİ: SeekBar'ı güncellemek için Handler ve Runnable
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    private val selectBackgroundLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            backgroundImageView.setImageURI(uri)
        } else {
            Toast.makeText(this, "Görsel seçilmedi.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)
        findViewById<android.view.View>(R.id.main)

        playPauseButton = findViewById(R.id.playPauseButton)
        changeBackgroundButton = findViewById(R.id.changeBackgroundButton)
        backgroundImageView = findViewById(R.id.backgroundImageView)
        seekBar = findViewById(R.id.seekBar) // SeekBar'ı XML'den bağla

        songUriString = intent.getStringExtra("SONG_URI")
        lyricsText = intent.getStringExtra("LYRICS_TEXT")

        if (songUriString != null && lyricsText != null) {
            Log.d("EditorActivity", "Veriler başarıyla alındı.")
            setupMediaPlayer()
        } else {
            Log.e("EditorActivity", "Gerekli veriler (URI veya Şarkı Sözü) alınamadı!")
            Toast.makeText(this, "Hata: Şarkı bilgileri alınamadı.", Toast.LENGTH_LONG).show()
            finish()
        }

        playPauseButton.setOnClickListener {
            togglePlayPause()
        }

        changeBackgroundButton.setOnClickListener {
            selectBackgroundLauncher.launch("image/*")
        }

        // YENİ: SeekBar dinleyicisini ayarla
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Kullanıcı çubuğu hareket ettirirken
                if (fromUser) {
                    mediaPlayer?.seekTo(progress) // Şarkıyı o an'a gönder
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Kullanıcı çubuğa ilk dokunduğunda
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Kullanıcı parmağını çubuktan kaldırdığında
            }
        })
    }

    private fun setupMediaPlayer() {
        mediaPlayer = MediaPlayer()
        try {
            mediaPlayer?.setDataSource(this, Uri.parse(songUriString))
            mediaPlayer?.prepareAsync()
            mediaPlayer?.setOnPreparedListener { player ->
                // YENİ: SeekBar'ın maksimum değerini şarkının süresine eşitle
                seekBar.max = player.duration
                player.start()
                playPauseButton.setImageResource(android.R.drawable.ic_media_pause)

                // YENİ: SeekBar güncelleme döngüsünü başlat
                startSeekBarUpdate()
            }
            mediaPlayer?.setOnCompletionListener {
                playPauseButton.setImageResource(android.R.drawable.ic_media_play)
                // YENİ: Şarkı bittiğinde döngüyü durdur
                handler.removeCallbacks(runnable)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Hata: Medya oynatıcısı kurulamadı.", Toast.LENGTH_LONG).show()
        }
    }

    // YENİ: Bu fonksiyon SeekBar'ı güncelleyen döngüyü başlatır
    private fun startSeekBarUpdate() {
        runnable = Runnable {
            mediaPlayer?.let {
                if(it.isPlaying) {
                    seekBar.progress = it.currentPosition
                    handler.postDelayed(runnable, 500) // Her 500 milisaniyede bir tekrarla
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
                // YENİ: Duraklatıldığında döngüyü durdur
                handler.removeCallbacks(runnable)
            } else {
                player.start()
                playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
                // YENİ: Devam ettiğinde döngüyü yeniden başlat
                startSeekBarUpdate()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        // YENİ: Aktivite yok edildiğinde döngüyü tamamen durdur
        handler.removeCallbacks(runnable)
    }
}