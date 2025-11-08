package com.example.radio

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.radio.model.Program
import com.example.radio.presenter.ProgramsPresenter
import com.example.radio.view.ProgramsView
import com.google.firebase.database.*

class ProgramsActivity : AppCompatActivity(), ProgramsView {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProgramAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var presenter: ProgramsPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_programs)

        findViewById<ImageButton>(R.id.btnmenu).visibility = View.GONE
        val btn_back_main = findViewById<ImageButton>(R.id.back_menu_main)
        btn_back_main.visibility = View.VISIBLE
        btn_back_main.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        supportActionBar?.setDisplayShowTitleEnabled(false)

        recyclerView = findViewById(R.id.recyclerPrograms)
        progressBar = findViewById(R.id.progressBar)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ProgramAdapter(emptyList())
        recyclerView.adapter = adapter

        // Inicializar el presenter
        presenter = ProgramsPresenter(this)

        // Cargar datos usando presenter
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        presenter.loadPrograms()
    }

    // Implementación de ProgramsView
    override fun showPrograms(programs: List<Program>) {
        adapter.updateData(programs)
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    override fun showProgramsError(message: String) {
        Log.e("ProgramsActivity", message)
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    // Adaptador (igual que antes)
    private inner class ProgramAdapter(
        private var programs: List<Program>
    ) : RecyclerView.Adapter<ProgramAdapter.ProgramViewHolder>() {

        inner class ProgramViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val txtTitle: TextView = view.findViewById(R.id.txtTitle)
            val txtDescription: TextView = view.findViewById(R.id.txtDescription)
            val txtSchedule: TextView = view.findViewById(R.id.txtSchedule)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ProgramViewHolder(layoutInflater.inflate(R.layout.item_radio_program, parent, false))

        override fun onBindViewHolder(holder: ProgramViewHolder, position: Int) {
            val program = programs[position]
            holder.txtTitle.text = program.name_program
            holder.txtDescription.text = program.description_program
            holder.txtSchedule.text = program.data_time

            // Obtener el ImageView
            val imgProgram = holder.itemView.findViewById<ImageView>(R.id.img_program)

            // Lista de recursos de imágenes para ciclar
            val images = listOf(
                R.drawable.item_background_program_detail,
                R.drawable.item_background_program_detail_3,
                R.drawable.item_background_program_detail_2
            )

            // Seleccionar la imagen según la posición (ciclo)
            val imageRes = images[position % images.size]

            // Asignar la imagen al ImageView
            imgProgram.setImageResource(imageRes)
        }


        override fun getItemCount(): Int = programs.size

        fun updateData(newList: List<Program>) {
            programs = newList
            notifyDataSetChanged()
        }
    }
}
