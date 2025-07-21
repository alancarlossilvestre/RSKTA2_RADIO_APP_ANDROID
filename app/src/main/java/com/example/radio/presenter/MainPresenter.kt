package com.example.radio.presenter

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.radio.model.StreamModel
import com.example.radio.view.MainView
import java.net.HttpURLConnection
import java.net.URL
import com.example.radio.model.AuthModel
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.DefaultLoadControl
import com.example.radio.MainActivity
import com.google.firebase.auth.FirebaseAuth


class MainPresenter(private val context: Context, private val view: MainView){

    private var exoPlayer: ExoPlayer? = null
    private var isPlaying = false
    private val stream = StreamModel()
    private val TAG = "RadioAppLog"

    fun togglePlayback() {
        if (exoPlayer == null) {
            initializePlayer()
        } else {
            stopPlayback()
        }
    }

     fun initializePlayer(retryCount: Int = 0) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            view.showLoading()
        }
        Thread {
            try {
                val url = URL(stream.streamUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Icy-MetaData", "1")
                connection.setRequestProperty("User-Agent", "ExoPlayer")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.connect()

                val responseCode = connection.responseCode
                Log.d(TAG, "Código de respuesta del servidor: $responseCode")
                if (responseCode in 200..399) {
                    // Servidor accesible → reproducir
                    android.os.Handler(android.os.Looper.getMainLooper()).post{
                       startPlayback()
                        view.hideLoading()
                    }
                } else {
                    Log.e(TAG, "Servidor no disponible, código HTTP: $responseCode")
                }

                connection.disconnect()

            } catch (e: Exception) {
                Log.e(TAG, "Error al conectar con el stream", e)
                retryConnection(retryCount)
            }
        }.start()
    }

    private fun startPlayback() {
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse(stream.streamUrl))
            setMediaItem(mediaItem)
            prepare()
            play()

        }

        isPlaying = true
        view.showStatusMessage("Reproduciendo...")
        view.updatePlayButton("Detener")

    }

    private fun stopPlayback() {
        exoPlayer?.release()
        exoPlayer = null
        isPlaying = false
        view.showStatusMessage("reproducción detenida")
        view.updatePlayButton("Reproducir")
    }
    fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }

    fun loginWithGoogle() {
        val authModel = AuthModel(context)
        authModel.loginWithGoogle(
            onSuccess = {
                (context as? MainActivity)?.runOnUiThread {
                    view.showLoginSuccess()
                    view.hideLoginButton()
                    view.showLogoutButton()
                    view.navigateToComentsFragment()
                    // Podrías redirigir a otra pantalla aquí si lo deseas
                }
            },
            onFailure = { error ->
                (context as? MainActivity)?.runOnUiThread {
                    view.showLoginError(error)
                }
            }
        )
    }

    fun onUserAlreadyLoggedIn() {
        view.hideLoginButton()
        view.showLogoutButton()
        view.navigateToComentsFragment()
    }


    fun onLogoutClicked() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val name = currentUser?.displayName ?: "usuario"
        view.showGoodbyeMessage(name)

        FirebaseAuth.getInstance().signOut()
        view.showLoginButton()
        view.hideLogoutButton()
        view.removeCommentsFragment()
    }

    private fun retryConnection(retryCount: Int) {
        if (retryCount < 3) { // Máximo 3 reintentos
            Log.d(TAG, "Reintentando conexión... intento ${retryCount + 1}")
            Thread.sleep(2000) // Espera antes de reintentar
            initializePlayer(retryCount + 1)
        } else {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                view.hideLoading()
                view.showStatusMessage("No se pudo conectar al stream. Intenta más tarde.")
            }
        }
    }

}