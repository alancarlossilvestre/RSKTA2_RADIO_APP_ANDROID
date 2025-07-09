package com.example.radio

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.radio.presenter.MainPresenter
import com.example.radio.view.MainView


class MainActivity : AppCompatActivity(), MainView{

    private lateinit var btnPlay: Button
    private lateinit var txtStatus: TextView
    private lateinit var presenter: MainPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnPlay = findViewById(R.id.btnPlayStream)
        txtStatus = findViewById(R.id.txtStatus)

        presenter = MainPresenter(this, this)

        btnPlay.setOnClickListener {
            presenter.togglePlayback()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.releasePlayer()
    }

    override fun updatePlayButton(text: String) {
        btnPlay.text = text
    }

    override fun showStatusMessage(message: String) {
        txtStatus.text = message
    }

}
