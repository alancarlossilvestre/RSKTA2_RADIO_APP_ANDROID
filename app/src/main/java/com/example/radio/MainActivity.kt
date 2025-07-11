package com.example.radio

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.radio.presenter.MainPresenter
import com.example.radio.view.MainView
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity(), MainView{

    private lateinit var btnPlay: Button
    private lateinit var txtStatus: TextView
    private lateinit var presenter: MainPresenter

    private lateinit var googlebtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnPlay = findViewById(R.id.btnPlayStream)
        txtStatus = findViewById(R.id.txtStatus)

        googlebtn = findViewById(R.id.google_sign_in_button_id)

        presenter = MainPresenter(this, this)

        btnPlay.setOnClickListener {
            presenter.togglePlayback()
        }

        googlebtn.setOnClickListener{
            setContent {
                login_google()
            }
        }
    }

    @SuppressLint("CoroutineCreationDuringComposition")
    @Composable
    fun login_google(){
        val context = LocalContext.current
        val coroutineScope:CoroutineScope= rememberCoroutineScope()
        val credentialManager= CredentialManager.create(context)
        val signInWithGoogleOption: GetSignInWithGoogleOption = GetSignInWithGoogleOption.Builder(getString(R.string.web_client))
            .setNonce("nonce")
        .build()
        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        coroutineScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = context,
                )
                handleSignIn(result)
            } catch (e: GetCredentialException) {
                Toast.makeText(context,"usuario o contraseña incorrecto" +e, Toast.LENGTH_LONG).show()
            }
        }

    }

    fun handleSignIn(result: GetCredentialResponse) {
        // Handle the successfully returned credential.
        val credential = result.credential

        when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        // Use googleIdTokenCredential and extract id to validate and
                        // authenticate on your server.
                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)

                        val credencial=GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

                        FirebaseAuth.getInstance().signInWithCredential(credencial)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    // Iniciar la actividad principal
                                    val intent = Intent(this, MainActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                } else {
                                    // Manejo del error, por ejemplo mostrar un Toast
                                    Toast.makeText(this, "Error al iniciar sesión", Toast.LENGTH_SHORT).show()

                                }

                            }

                    } catch (e: GoogleIdTokenParsingException) {
                        //Log.e(TAG, "Received an invalid google id token response", e)
                        Toast.makeText(applicationContext,"Received an invalid google id token response"+e , Toast.LENGTH_LONG).show()
                    }
                }
                else {
                    // Catch any unrecognized credential type here.
                    //Log.e(TAG, "Unexpected type of credential")
                    Toast.makeText(applicationContext,"Unexpected type of credential" , Toast.LENGTH_LONG).show()
                }
            }

            else -> {
                // Catch any unrecognized credential type here.
                //Log.e(TAG, "Unexpected type of credential")
                Toast.makeText(applicationContext,"Unexpected type of credential" , Toast.LENGTH_LONG).show()
            }
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
