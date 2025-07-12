package com.example.radio

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*

import androidx.appcompat.app.AppCompatActivity

import com.example.radio.presenter.MainPresenter
import com.example.radio.view.MainView
import com.example.radio.R
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity(), MainView{

    private lateinit var btnPlay: Button
    private lateinit var txtStatus: TextView
    private lateinit var presenter: MainPresenter

    private lateinit var googlebtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        //verifica el inicio de sesion

        presenter = MainPresenter(this, this)

        //cerrar sesion
        val loginButton = findViewById<Button>(R.id.google_sign_in_button_id)
        val logoutButton = findViewById<Button>(R.id.btnLogout)

        loginButton.setOnClickListener {
            presenter.loginWithGoogle()
        }

        logoutButton.setOnClickListener {
            presenter.onLogoutClicked()
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

        btnPlay.setOnClickListener {
            presenter.togglePlayback()
        }

        googlebtn.setOnClickListener{
            presenter.loginWithGoogle()
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
        findViewById<Button>(R.id.google_sign_in_button_id).visibility = View.GONE

    }

    override fun showLoginButton() {
        findViewById<Button>(R.id.google_sign_in_button_id).visibility = View.VISIBLE

    }

    override fun showLogoutButton() {
        findViewById<Button>(R.id.btnLogout).visibility = View.VISIBLE
    }

    override fun hideLogoutButton() {
        findViewById<Button>(R.id.btnLogout).visibility = View.GONE
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
        Toast.makeText(this, "Adiós, $name 👋", Toast.LENGTH_SHORT).show()
    }


}
