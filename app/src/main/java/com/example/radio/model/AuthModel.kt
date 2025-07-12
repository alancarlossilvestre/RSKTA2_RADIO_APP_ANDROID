package com.example.radio.model

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.credentials.*
import androidx.credentials.exceptions.GetCredentialException
import com.example.radio.R
import com.google.android.libraries.identity.googleid.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.*

class AuthModel(private val context: Context) {

    private val webClientId = context.getString(R.string.web_client)

    fun loginWithGoogle(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val credentialManager = CredentialManager.create(context)

        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
            .setNonce("nonce")
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )
                handleCredential(result, onSuccess, onFailure)
            } catch (e: GetCredentialException) {
                Log.e("AuthModel", "Error de credencial: ${e.message}")
                withContext(Dispatchers.Main) {
                    onFailure("Error de autenticación: ${e.message}")
                }
            }
        }
    }

    private suspend fun handleCredential(
        result: GetCredentialResponse,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val credential = result.credential

        when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(credential.data)

                        val firebaseCredential = GoogleAuthProvider.getCredential(
                            googleIdTokenCredential.idToken, null
                        )

                        withContext(Dispatchers.Main) {
                            FirebaseAuth.getInstance()
                                .signInWithCredential(firebaseCredential)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        onSuccess()
                                    } else {
                                        onFailure("No se pudo iniciar sesión")
                                    }
                                }
                        }
                    } catch (e: GoogleIdTokenParsingException) {
                        onFailure("Error al procesar token: ${e.message}")
                    }
                } else {
                    onFailure("Tipo de credencial inesperado")
                }
            }

            else -> {
                onFailure("Credencial desconocida")
            }
        }
    }
}
