package com.example.radio.model

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.radio.MainActivity

class StreamingService : Service(), AudioManager.OnAudioFocusChangeListener {

    private lateinit var exoPlayer: ExoPlayer
    private var isPlaying = false
    private val streamUrl = "https://stream.zeno.fm/kfgt3srascitv"
    private val CHANNEL_ID = "StreamingServiceChannel"
    private val NOTIFICATION_ID = 1
    private val TAG = "StreamingService"
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        initializePlayer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_PLAY" -> startPlayback()
            "ACTION_STOP" -> stopPlayback()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun initializePlayer() {
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
            setMediaItem(mediaItem)
            prepare()
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_READY -> Log.d(TAG, "ExoPlayer listo para reproducir")
                        Player.STATE_BUFFERING -> Log.d(TAG, "ExoPlayer en buffering")
                        Player.STATE_ENDED -> Log.d(TAG, "ExoPlayer terminó")
                        Player.STATE_IDLE -> Log.d(TAG, "ExoPlayer inactivo")
                    }
                }
            })
        }
        Log.d(TAG, "Iniciando servicio en primer plano")
        try {
            startForeground(NOTIFICATION_ID, createNotification())
            Log.d(TAG, "Servicio en primer plano iniciado correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar servicio en primer plano", e)
        }
    }

    private fun startPlayback() {
        // Solicitar foco de audio
        val focusResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(this)
                .build()
            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        if (focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            if (exoPlayer.playbackState == Player.STATE_IDLE) {
                val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                Log.d(TAG, "ExoPlayer preparado nuevamente después de stop")
            }
            exoPlayer.play()
            isPlaying = true
            updateNotification("Reproduciendo...")
            handler.removeCallbacksAndMessages(null)
            Log.d(TAG, "Tareas de detención canceladas al iniciar reproducción")
        } else {
            Log.w(TAG, "Foco de audio no concedido, no se inicia la reproducción")
            updateNotification("Esperando foco de audio...")
        }
    }

    private fun stopPlayback() {
        exoPlayer.stop()
        isPlaying = false
        updateNotification("Reproducción detenida")
        // Programar la detención del servicio después de 15 minutos
        handler.postDelayed({
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                Log.d(TAG, "Servicio detenido y notificación eliminada después de 15 minutos")
            } catch (e: Exception) {
                Log.e(TAG, "Error al detener el servicio después de 15 minutos", e)
            }
        }, 5 * 60 * 1000L)
        Log.d(TAG, "Programada detención del servicio en 15 minutos")
        // Abandonar foco de audio
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(this)
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "Foco de audio perdido permanentemente, pausando reproducción")
                if (isPlaying) {
                    exoPlayer.pause()
                    isPlaying = false
                    updateNotification("Reproducción pausada (otro audio)")
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "Foco de audio perdido temporalmente, pausando reproducción")
                if (isPlaying) {
                    exoPlayer.pause()
                    isPlaying = false
                    updateNotification("Reproducción pausada (interrupción)")
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "Foco de audio perdido temporalmente (puede bajar volumen), pausando por simplicidad")
                if (isPlaying) {
                    exoPlayer.pause()
                    isPlaying = false
                    updateNotification("Reproducción pausada (interrupción)")
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "Foco de audio recuperado")
                // Opcional: Reanudar solo si estaba pausado por una interrupción temporal
                if (!isPlaying && exoPlayer.playbackState != Player.STATE_IDLE) {
                    exoPlayer.play()
                    isPlaying = true
                    updateNotification("Reproduciendo...")
                    handler.removeCallbacksAndMessages(null)
                    Log.d(TAG, "Reproducción reanudada automáticamente")
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Streaming Service",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Canal para notificaciones de reproducción de streaming"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            Log.d(TAG, "Canal de notificación creado: $CHANNEL_ID")
        }
    }

    private fun createNotification(status: String = "Conectando..."): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Radio Streaming")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (isPlaying) {
            val stopIntent = Intent(this, StreamingService::class.java).apply {
                action = "ACTION_STOP"
            }
            val stopPendingIntent = PendingIntent.getService(
                this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause,"Detener", stopPendingIntent)
        } else {
            val playIntent = Intent(this, StreamingService::class.java).apply {
                action = "ACTION_PLAY"
            }
            val playPendingIntent = PendingIntent.getService(
                this, 1, playIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, "Reproducir", playPendingIntent)
        }

        val notification = builder.build()
        Log.d(TAG, "Notificación creada con estado: $status, isPlaying: $isPlaying")
        return notification
    }

    private fun updateNotification(status: String) {
        try {
            val notification = createNotification(status)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Notificación actualizada con estado: $status")
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar notificación", e)
        }
    }

    override fun onDestroy() {
        exoPlayer.release()
        handler.removeCallbacksAndMessages(null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(this)
        }
        super.onDestroy()
        Log.d(TAG, "Servicio destruido")
    }
}