package com.example.radio

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.savedstate.serialization.saved
import com.example.radio.ui.theme.utils.CircleTransform
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.squareup.picasso.Picasso

class ProfileActivity : AppCompatActivity() {

    private lateinit var nombre: TextView
    private lateinit var nombre2: EditText
    //private lateinit var direccion: EditText
    private lateinit var email: EditText
    private lateinit var frase: EditText
    private lateinit var edad: EditText
    private lateinit var fotoPerfil: ImageView
    private lateinit var btn_edit_data: ImageButton
    private lateinit var btn_save_data: LinearLayout

    private lateinit var progressBar: View

    private var firebaseUser: FirebaseUser? = null

    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        progressBar = findViewById(R.id.progressBar)

        // Toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val btnMenu = toolbar.findViewById<ImageButton>(R.id.btnmenu)
        val btnBack = toolbar.findViewById<ImageButton>(R.id.back_menu_main)
        btnMenu.visibility = View.GONE
        btnBack.visibility = View.VISIBLE
        btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Vistas
        nombre = findViewById(R.id.viewNameTV)
        nombre2 = findViewById(R.id.viewName2)
        edad = findViewById(R.id.etedad)  // asegurate que exista en tu XML
        email = findViewById(R.id.viewtEmail)
        frase = findViewById(R.id.viewVerse)
        fotoPerfil = findViewById(R.id.profile_image_view)

        // Recibir datos del Intent
        val senderId = intent.getStringExtra("senderId")
        val senderName = intent.getStringExtra("senderName")
        val senderPhoto = intent.getStringExtra("senderPhoto")

        if (senderId != null) {
            // Buscar datos del usuario en Firebase Database
            loadUserDataFromDatabase(senderId, senderName, senderPhoto)
        } else {
            progressBar.visibility = View.VISIBLE
            //Si no se pasa senderId, mostrar usuario autenticado
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            firebaseUser?.let { user ->
                progressBar.visibility = View.GONE
                nombre.text = user.displayName ?: "Usuario Desconocido"
                nombre2.setText(user.displayName ?: "Usuario Desconocido")
                email.setText(user.email ?: "")

                Picasso.get()
                    .load(user.photoUrl)
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .transform(CircleTransform())
                    .into(fotoPerfil)
            }
        }

        btn_edit_data = findViewById(R.id.btn_edit_data)
        btn_save_data = findViewById(R.id.btn_save_data)

        btn_edit_data.setOnClickListener {
            edad.isEnabled = true
            edad.setBackgroundResource(R.drawable.btn_border)
            frase.isEnabled = true
            frase.setBackgroundResource(R.drawable.btn_border)

        }

        btn_save_data.setOnClickListener {
            edad.isEnabled = false
            edad.setBackgroundResource(R.drawable.border_profile_item)
            frase.isEnabled = false
            frase.setBackgroundResource(R.drawable.border_profile_item)

            senUpdateData()
        }
    }

    // Cargar datos del usuario desde la base de datos
    private fun loadUserDataFromDatabase(uid: String, senderName: String?, senderPhoto: String?) {

        progressBar.visibility = View.VISIBLE

        findViewById<ImageView>(R.id.btn_edit_data).visibility = View.GONE
        findViewById<LinearLayout>(R.id.btn_save_data).visibility = View.GONE

        val userRef = database.child("users").child(uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                progressBar.visibility = View.GONE
                if (snapshot.exists()) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: senderName
                    val verse = snapshot.child("verse").getValue(String::class.java)
                    val emailValue = snapshot.child("email").getValue(String::class.java)
                    val address = snapshot.child("address").getValue(String::class.java)
                    val photoUrl = snapshot.child("photoUrl").getValue(String::class.java) ?: senderPhoto

                    // Mostrar los datos
                    nombre.text = name ?: "Usuario Desconocido"
                    nombre2.setText(name ?: "")
                    frase.setText(verse ?: "Sin frase bíblica")
                    email.setText(emailValue ?: "")
                    //direccion.setText(address ?: "")

                    Picasso.get()
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_default_profile)
                        .error(R.drawable.ic_default_profile)
                        .transform(CircleTransform())
                        .into(fotoPerfil)
                } else {
                    Toast.makeText(this@ProfileActivity, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProfileActivity, "Error al cargar perfil: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun senUpdateData() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "No hay usuario autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = user.uid
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid)

        // Obtener valores actualizados
        val updatedAge = edad.text.toString().trim()
        val updatedVerse = frase.text.toString().trim()

        // Crear mapa con los valores actualizados
        val updates = mapOf(
            "verse" to updatedVerse,
            "age" to updatedAge,
            "photoUrl" to (user.photoUrl?.toString() ?: "")
        )

        progressBar.visibility = View.VISIBLE

        // Actualizar en Firebase
        userRef.updateChildren(updates)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()

            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


}
