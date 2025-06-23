package com.codenzi.lyricvideocreator

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class LyricsActivity : AppCompatActivity() {

    private var selectedSongUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lyrics)
        // Hata vermemesi için main ID'li root layout'u buluyoruz (sadece insets için)
        findViewById<android.view.View>(R.id.main)

        // 1. MainActivity'den gönderilen şarkı URI'sini al
        selectedSongUri = intent.getStringExtra("SONG_URI")

        // 2. Arayüzdeki elemanları tanımla
        val lyricsInputEditText: EditText = findViewById(R.id.lyricsInputEditText)
        val confirmLyricsButton: Button = findViewById(R.id.confirmLyricsButton)

        // 3. "Devam Et" butonuna tıklandığında...
        confirmLyricsButton.setOnClickListener {
            val lyricsText = lyricsInputEditText.text.toString()

            // 4. Şarkı sözü kutusunun boş olup olmadığını kontrol et
            if (lyricsText.isBlank()) {
                Toast.makeText(this, "Lütfen şarkı sözlerini girin.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Kodu burada durdur
            }

            // 5. Her şey yolundaysa, EditorActivity'e gitmek için yeni bir Intent oluştur
            val intent = Intent(this, EditorActivity::class.java).apply {
                // Hem şarkı URI'sini hem de şarkı sözlerini bu Intent'e ekle
                putExtra("SONG_URI", selectedSongUri)
                putExtra("LYRICS_TEXT", lyricsText)
            }
            // Yolculuğu başlat!
            startActivity(intent)
        }
    }
}