package com.example.radio.presenter

import android.util.Log
import com.example.radio.model.Program
import com.example.radio.view.ProgramsView
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class ProgramsPresenter(private val view: ProgramsView) {

    // Usa la misma URL que AuthModel
    private val database = Firebase.database("https://world-news-6767b-default-rtdb.firebaseio.com/")
    private val programsRef = database.getReference("programs")

    fun loadPrograms() {
        programsRef.child("program_id:").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val programList = snapshot.getValue(object : GenericTypeIndicator<List<Program>>() {}) ?: emptyList()

                // Filtramos los null que vienen en la lista
                val filteredList = programList.filterNotNull()

                Log.d("ProgramsPresenter", "Programas cargados: ${filteredList.size}")
                view.showPrograms(filteredList)
            }

            override fun onCancelled(error: DatabaseError) {
                view.showProgramsError("Error al cargar programas: ${error.message}")
                Log.e("ProgramsPresenter", "Firebase error: ${error.message}")
            }
        })
    }
}
