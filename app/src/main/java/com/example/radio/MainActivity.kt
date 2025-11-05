package com.example.radio

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.animate
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable

import com.example.radio.presenter.MainPresenter
import com.example.radio.view.MainView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity(), MainView{

    private lateinit var btnPlay: ImageView
    private lateinit var txtStatus: TextView
    private lateinit var presenter: MainPresenter
    private lateinit var progressBar: ProgressBar
    private lateinit var googlebtn: LinearLayout
    private lateinit var menu: ImageButton
    private lateinit var back_menu: ImageButton
    private lateinit var btn_share: ImageButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        //verifica el inicio de sesion
        presenter = MainPresenter(this, this)

        //cerrar sesion
        val loginButton = findViewById<LinearLayout>(R.id.google_sign_in_button_id)

        loginButton.setOnClickListener {
            presenter.loginWithGoogle()
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null){
            //el usuario ya esta logeado
            Log.d("AuthCheck", "Usuario ya logueado: ${currentUser.email}")
            val displayName = currentUser.displayName ?: "Usuario"
            Toast.makeText(this, "¡Bienvenido, $displayName!", Toast.LENGTH_SHORT).show()
            presenter.onUserAlreadyLoggedIn()

        }

        btnPlay = findViewById(R.id.btnPlayStream)
        txtStatus = findViewById(R.id.txtStatus)

        googlebtn = findViewById(R.id.google_sign_in_button_id)

        presenter = MainPresenter(this, this)

        progressBar = findViewById(R.id.progressBar)

        menu = findViewById(R.id.btnmenu)

        menu.setOnClickListener {
            openMenuFragment()
        }

        back_menu = findViewById(R.id.back_menu)

        back_menu.setOnClickListener {
            back_activityMain()
        }

        btnPlay.setOnClickListener {
            presenter.togglePlayback()
        }

        googlebtn.setOnClickListener{
            presenter.loginWithGoogle()
        }

        btn_share = findViewById(R.id.icono_compartir)

        btn_share.setOnClickListener {
            presenter.shareapp()
        }

        presenter.initializePlayer()

        val programsFragment = ProgramsFragment.newInstance("", "")
        supportFragmentManager.beginTransaction()
            .replace(R.id.programs_fragment, programsFragment)
            .commit()

    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.releasePlayer()
    }

    override fun updatePlayButton(isPlaying: Boolean) {
        val btnPlayStream = findViewById<ImageButton>(R.id.btnPlayStream)

        val barraAudio = findViewById<LottieAnimationView>(R.id.barraAudio)

        if (isPlaying) {
            // Cambia a icono de pausa
            btnPlayStream.setImageResource(android.R.drawable.ic_media_pause)


            barraAudio.repeatCount = LottieDrawable.INFINITE
            barraAudio.playAnimation()

        } else {
            // Cambia a icono de play
            btnPlayStream.setImageResource(android.R.drawable.ic_media_play)
            barraAudio.pauseAnimation()
        }
    }

    override fun showStatusMessage(message: String) {
        txtStatus.text = message
    }

    override fun showLoginSuccess() {

        Toast.makeText(this, "Inicio de sesion exitoso", Toast.LENGTH_SHORT).show()
    }

    override fun showLoginError(message: String) {
        Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
        Log.e("AuthModel", "Error de credencial: ${message}")
    }

    override fun navigateToComentsFragment() {

        val container = findViewById<FrameLayout>(R.id.comments_fragment)
        container.visibility = View.VISIBLE

        val transaction = supportFragmentManager.beginTransaction()
        //animacion
        transaction.setCustomAnimations(
            android.R.anim.fade_in,
            android.R.anim.fade_out,
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
        transaction.replace(R.id.comments_fragment, CommentsFragment())
        transaction.commit()
    }

    override fun hideLoginButton() {
        findViewById<LinearLayout>(R.id.google_sign_in_button_id).visibility = View.GONE

    }

    override fun showLoginButton() {
        findViewById<LinearLayout>(R.id.google_sign_in_button_id).visibility = View.VISIBLE

    }

    override fun removeCommentsFragment() {
        val fragment = supportFragmentManager.findFragmentById(R.id.comments_fragment)
        if (fragment != null) {
            supportFragmentManager.beginTransaction()
                .remove(fragment)
                .commit()
        }
        findViewById<FrameLayout>(R.id.comments_fragment).visibility = View.GONE
    }

    override fun showGoodbyeMessage(name: String) {
        findViewById<View>(R.id.fragment_menu).visibility = View.GONE
        Toast.makeText(this, "Adiós, $name 👋", Toast.LENGTH_SHORT).show()
    }

    override fun showLoading() {
        progressBar.visibility = View.VISIBLE
    }

    override fun hideLoading() {
        progressBar.visibility = View.GONE
    }
    // Launcher para permisos
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Log.e("MainActivity", "Permiso POST_NOTIFICATIONS no concedido")
            Toast.makeText(this, "Se requieren permisos de notificación para el streaming", Toast.LENGTH_LONG).show()
        } else {
            Log.d("MainActivity", "Permiso POST_NOTIFICATIONS concedido")
        }
    }

    private fun openMenuFragment() {
        val transaction = supportFragmentManager.beginTransaction()

        // Animaciones al abrir y al cerrar el fragment
        transaction.setCustomAnimations(
            android.R.anim.fade_in,   // enter
            android.R.anim.fade_out,  // exit
            android.R.anim.fade_in,   // popEnter -> animación al volver al fragment anterior
            android.R.anim.fade_out   // popExit  -> animación al salir del fragment actual
        )

        transaction.replace(R.id.fragment_menu, MenuFragment())
        transaction.addToBackStack(null)
        transaction.commit()

        // Actualizar visibilidad de botones
        menu.visibility = View.GONE
        findViewById<ImageButton>(R.id.back_menu).visibility = View.VISIBLE
        findViewById<View>(R.id.fragment_menu).visibility = View.VISIBLE
    }

    fun back_activityMain() {
        // Solo hacemos popBackStack, la animación de salida está definida en popExit
        supportFragmentManager.popBackStack()

        // Actualizamos visibilidad de botones
        back_menu.visibility = View.GONE
        menu.visibility = View.VISIBLE
    }

}
