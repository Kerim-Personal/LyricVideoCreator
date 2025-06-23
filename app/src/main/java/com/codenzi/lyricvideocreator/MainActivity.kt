package com.codenzi.lyricvideocreator

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity() {

    // Dosya seçiciden dönen sonucu karşılayacak olan "posta kutusu"
    private val selectSongLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // Kullanıcı bir dosya seçtiğinde veya seçimi iptal ettiğinde bu blok çalışır.
        if (uri != null) {
            // Eğer bir dosya başarıyla seçildiyse (uri null değilse):
            // Bir sonraki ekrana gitmek için bir "Intent" oluştur.
            val intent = Intent(this, LyricsActivity::class.java).apply {
                // Seçilen dosyanın adresini (URI) "SONG_URI" anahtarıyla bu Intent'e ekle.
                // URI'yi String'e çevirerek gönderiyoruz.
                putExtra("SONG_URI", uri.toString())
            }
            // Hazırlanan Intent ile LyricsActivity'yi başlat.
            startActivity(intent)
        } else {
            // Eğer kullanıcı dosya seçmekten vazgeçtiyse (uri null ise):
            // Ekranda kısa bir bilgilendirme mesajı göster.
            Toast.makeText(this, "Şarkı seçilmedi.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // XML'deki butonu koddaki bir değişkene bağla.
        val selectSongButton: Button = findViewById(R.id.selectSongButton)

        // Butona tıklandığında ne olacağını tanımla.
        selectSongButton.setOnClickListener {
            // Tıklandığında, yukarıda tanımladığımız dosya seçiciyi başlat.
            // "audio/*" parametresi, sadece ses dosyalarının gösterilmesini sağlar.
            selectSongLauncher.launch("audio/*")
        }
    }
}