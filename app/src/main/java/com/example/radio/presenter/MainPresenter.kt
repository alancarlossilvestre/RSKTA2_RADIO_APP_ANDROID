package com.example.radio.presenter

import android.content.Context
import android.content.Intent
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
import com.example.radio.model.StreamingService
import com.google.firebase.auth.FirebaseAuth


class MainPresenter(private val context: Context, private val view: MainView){
    private var isPlaying = false
    private val stream = StreamModel()
    private val TAG = "RadioAppLog"

    fun togglePlayback() {
        if (isPlaying) {
            stopPlayback()
        } else {
            startPlayback()
        }
    }

    fun initializePlayer() {
        view.showLoading()
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
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        startPlayback()
                        view.hideLoading()
                    }
                } else {
                    Log.e(TAG, "Servidor no disponible, código HTTP: $responseCode")
                    retryConnection(0)
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error al conectar con el stream", e)
                retryConnection(0)
            }
        }.start()
    }

    private fun startPlayback() {
        val intent = Intent(context, StreamingService::class.java).apply {
            action = "ACTION_PLAY"
        }
        context.startService(intent)
        isPlaying = true
        view.showStatusMessage("Reproduciendo...")
        view.updatePlayButton("Detener")
    }

    private fun stopPlayback() {
        val intent = Intent(context, StreamingService::class.java).apply {
            action = "ACTION_STOP"
        }
        context.startService(intent)
        isPlaying = false
        view.showStatusMessage("Reproducción detenida")
        view.updatePlayButton("Reproducir")
    }

    fun releasePlayer() {
        stopPlayback()
    }

    private fun retryConnection(retryCount: Int) {
        if (retryCount < 3) {
            Log.d(TAG, "Reintentando conexión... intento ${retryCount + 1}")
            Thread.sleep(2000)
            initializePlayer()
        } else {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                view.hideLoading()
                view.showStatusMessage("No se pudo conectar al stream. Intenta más tarde.")
            }
        }
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


}