package com.example.radio

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.radio.R
import com.example.radio.ui.theme.utils.CircleTransform
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import org.w3c.dom.Comment

class CommentsFragment : Fragment() {
    private lateinit var ic_my_profile: ImageView
    private lateinit var etComment: EditText
    private lateinit var ibSend: ImageButton
    private lateinit var rvComments: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private var firebaseUser: FirebaseUser? = null
    private lateinit var commentsAdapter: CommentsAdapter
    private val commentsList = mutableListOf<Comment>()

    // Ajusta contentId según cómo obtengas el ID del contenido (por ejemplo, desde argumentos)
    private val contentId: String by lazy {
        arguments?.getString("contentId") ?: "default_content_id"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_comments, container, false)
        initViews(view)
        setupRecyclerView()
        loadComments()
        firebaseUser = FirebaseAuth.getInstance().currentUser

        ibSend.setOnClickListener {
            val message = etComment.text.toString().trim()
            if (message.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor escribe un comentario", Toast.LENGTH_SHORT).show()
            } else {
                sendComment(firebaseUser?.uid, message)
                etComment.text.clear()
            }
        }
        //cargo imagen de perfil
        firebaseUser?.photoUrl?.let { photoUrl ->
            Log.d("Perfil Debug", "cargando iamgen desde URL:  $photoUrl")
            Picasso.get()
                .load(photoUrl)
                .placeholder(R.drawable.ic_default_profile)
                .error(R.drawable.ic_default_profile)
                .transform(CircleTransform())
                .into(ic_my_profile)
        } ?: run {
            Log.d("DEBUG", "No hay URL de foto, usando imagen por defecto")
            // If no photo URL, set a default image
            ic_my_profile.setImageResource(R.drawable.ic_default_profile)
        }

        return view
    }

    private fun initViews(view: View) {
        etComment = view.findViewById(R.id.et_comment)
        ibSend = view.findViewById(R.id.ib_send)
        rvComments = view.findViewById(R.id.rv_comments)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmptyState = view.findViewById(R.id.tv_empty_state)
        ic_my_profile = view.findViewById(R.id.ib_attach)
    }

    private fun setupRecyclerView() {
        commentsAdapter = CommentsAdapter(commentsList)
        rvComments.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = commentsAdapter
        }
        // Desplazar al último comentario cuando se agrega uno nuevo
        rvComments.scrollToPosition(commentsList.size - 1)
    }

    private fun loadComments() {
        progressBar.visibility = View.VISIBLE
        val commentsRef = FirebaseDatabase.getInstance()
            .getReference("comments/$contentId")

        commentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                commentsList.clear()
                for (commentSnapshot in snapshot.children) {
                    val comment = commentSnapshot.getValue(Comment::class.java)
                    comment?.let { commentsList.add(it) }
                }
                commentsAdapter.notifyDataSetChanged()
                progressBar.visibility = View.GONE
                tvEmptyState.visibility = if (commentsList.isEmpty()) View.VISIBLE else View.GONE
                // Desplazar al último comentario
                rvComments.scrollToPosition(commentsList.size - 1)
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Error al cargar comentarios: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendComment(uid: String?, message: String) {
        if (uid == null) {
            Toast.makeText(requireContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val reference = FirebaseDatabase.getInstance().getReference("comments/$contentId")
        val commentId = reference.push().key ?: return

        val comment = hashMapOf<String, Any?>(
            "commentId" to commentId,
            "senderId" to uid,
            "senderName" to firebaseUser?.displayName,
            "senderPhoto" to firebaseUser?.photoUrl?.toString(),
            "content" to message,
            "timestamp" to ServerValue.TIMESTAMP
        )

        reference.child(commentId).setValue(comment)
            .addOnSuccessListener {
                // Comentario enviado, no se necesita acción adicional
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al enviar comentario: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Modelo de datos para un comentario
    data class Comment(
        val commentId: String? = null,
        val senderId: String? = null,
        val senderName: String? = null,
        val senderPhoto: String? = null,
        val content: String? = null,
        val timestamp: Long? = null
    )

    // Adaptador para el RecyclerView
    inner class CommentsAdapter(private val comments: List<Comment>) :
        RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

        inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivProfile: ImageView = itemView.findViewById(R.id.iv_profile)
            val tvUsername: TextView = itemView.findViewById(R.id.tv_username)
            val tvTimestamp: TextView = itemView.findViewById(R.id.tv_timestamp)
            val tvComment: TextView = itemView.findViewById(R.id.tv_comment)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_comment, parent, false)
            return CommentViewHolder(view)
        }

        override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
            val comment = comments[position]
            holder.tvUsername.text = comment.senderName ?: "Anónimo"
            holder.tvComment.text = comment.content
            holder.tvTimestamp.text = comment.timestamp?.let { formatTimestamp(it) } ?: "Ahora"

            // Cargar foto de perfil sin Glide
            comment.senderPhoto?.let { photoUrl ->
                Picasso.get()
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .transform(CircleTransform())
                    .into(holder.ivProfile)
            } ?: holder.ivProfile.setImageResource(R.drawable.ic_default_profile)
        }

        override fun getItemCount(): Int = comments.size

        private fun formatTimestamp(timestamp: Long): String {
            val currentTime = System.currentTimeMillis()
            val diff = currentTime - timestamp
            return when {
                diff < 60_000 -> "hace un momento"
                diff < 3_600_000 -> "${diff / 60_000} minutos atrás"
                diff < 86_400_000 -> "${diff / 3_600_000} horas atrás"
                else -> "${diff / 86_400_000} días atrás"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpiar listeners para evitar memory leaks
       // FirebaseDatabase.getInstance().getReference("comments/$contentId")
       //     .removeEventListener { /* No es necesario implementar, solo asegura limpieza */ }
    }
}