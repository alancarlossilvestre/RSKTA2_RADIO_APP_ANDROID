package com.example.radio

import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class MainActivity : AppCompatActivity(){

    private var exoPlayer: ExoPlayer? = null
    private val streamUrl = "http://192.168.18.32:8000/stream" // tu URL de Icecast

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnPlay = findViewById<Button>(R.id.btnPlayStream)

        btnPlay.setOnClickListener {
            if (exoPlayer == null) {
                exoPlayer = ExoPlayer.Builder(this).build()
                val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
                exoPlayer?.setMediaItem(mediaItem)
                exoPlayer?.prepare()
                exoPlayer?.play()
                btnPlay.text = "Detener"
            } else {
                exoPlayer?.release()
                exoPlayer = null
                btnPlay.text = "Reproducir Icecast"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
    }
}
