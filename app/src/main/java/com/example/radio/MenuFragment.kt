package com.example.radio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.example.radio.presenter.MainPresenter
import com.example.radio.presenter.ProgramsPresenter
import com.example.radio.view.MainView
import com.google.firebase.auth.FirebaseAuth


class MenuFragment : Fragment() {

    private lateinit var presenter: MainPresenter
    private lateinit var logoutButton: LinearLayout
    private lateinit var btn_perfil: TextView
    private lateinit var btn_programs: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_menu, container, false)
    }
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            presenter = MainPresenter(requireActivity() as MainView as Context, requireContext() as MainView
            )

            logoutButton = view.findViewById(R.id.btnLogout)

            logoutButton.setOnClickListener {
                presenter.onLogoutClicked()
                logoutButton.visibility = View.GONE
                (requireActivity() as? MainActivity)?.showLoginButton()
                (requireActivity() as? MainActivity)?.back_activityMain()
            }

            // opcional: mostrar/ocultar según si hay usuario logueado
            updateLogoutVisibility()

            btn_perfil = view.findViewById(R.id.item_perfil)

            btn_perfil.setOnClickListener {
                val intent = Intent(requireContext(), ProfileActivity::class.java)
                startActivity(intent)

            }
            btn_programs = view.findViewById(R.id.item_programacion)

            btn_programs.setOnClickListener {
                val intent = Intent(requireContext(), ProgramsActivity::class.java)
                startActivity(intent)
            }
        }

        private fun updateLogoutVisibility() {
            val user = FirebaseAuth.getInstance().currentUser
            logoutButton.visibility = if (user != null) View.VISIBLE else View.GONE
        }
    }