package com.example.radio

import android.content.Intent
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
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.radio.R
import com.example.radio.ui.theme.utils.CircleTransform
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.squareup.picasso.Picasso

class CommentsFragment : Fragment() {
    private lateinit var ic_my_profile: ImageView
    private lateinit var etComment: EditText
    private lateinit var ibSend: ImageView
    private lateinit var rvComments: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private var firebaseUser: FirebaseUser? = null
    private lateinit var commentsAdapter: CommentsAdapter
    private val commentsList = mutableListOf<Comment>()
    private lateinit var btn_expand_comments: ImageButton
    private lateinit var btn_contract_comments: ImageButton
    private lateinit var cardview_Comments: CardView
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
        firebaseUser = FirebaseAuth.getInstance().currentUser

        // Cargar comentarios
        showLoading(true)
        loadComments()

        // Manejo de foco en input
        etComment.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                rvComments.post {
                    rvComments.scrollToPosition(commentsAdapter.itemCount - 1)
                }
            }
        }

        ibSend.setOnClickListener {
            val message = etComment.text.toString().trim()
            if (message.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor escribe un comentario", Toast.LENGTH_SHORT).show()
            } else {
                sendComment(firebaseUser?.uid, message)
                etComment.text.clear()
            }
        }

        ic_my_profile.setOnClickListener {
                val intent = Intent(requireContext(), ProfileActivity::class.java)
                startActivity(intent)
        }

        // Cargar imagen de perfil
        firebaseUser?.photoUrl?.let { photoUrl ->
            Log.d("Perfil Debug", "Cargando imagen desde URL: $photoUrl")
            Picasso.get()
                .load(photoUrl)
                .placeholder(R.drawable.ic_default_profile)
                .error(R.drawable.ic_default_profile)
                .transform(CircleTransform())
                .into(ic_my_profile)
        } ?: ic_my_profile.setImageResource(R.drawable.ic_default_profile)

        // Configuración de botones de expandir/contraer
        btn_expand_comments = view.findViewById(R.id.icon_expand)
        btn_contract_comments = view.findViewById(R.id.icon_contract)
        cardview_Comments = view.findViewById(R.id.cardView_Comments)

        btn_contract_comments.setOnClickListener {
            val params = cardview_Comments.layoutParams
            val scale = cardview_Comments.context.resources.displayMetrics.density
            params.height = (280 * scale).toInt()
            cardview_Comments.layoutParams = params

            btn_contract_comments.visibility = View.GONE
            btn_expand_comments.visibility = View.VISIBLE
            rvComments.post {
                val count = commentsAdapter.getItemCount()
                if (count > 0) {
                    rvComments.smoothScrollToPosition(count - 1)
                }
            }
        }

        val scrollViewMain = activity?.findViewById<ScrollView>(R.id.scrollMain)
        btn_expand_comments.setOnClickListener {
            scrollViewMain?.post {
                val params = cardview_Comments.layoutParams
                val scrollViewHeight = scrollViewMain.height ?: 0
                params.height = (scrollViewHeight * 0.8).toInt()
                cardview_Comments.layoutParams = params

                btn_expand_comments.visibility = View.GONE
                btn_contract_comments.visibility = View.VISIBLE
                val count = commentsAdapter.getItemCount()
                if (count > 0) {
                    rvComments.smoothScrollToPosition(count - 1)
                }
            }
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
    }

    private fun loadComments() {
        val commentsRef = FirebaseDatabase.getInstance().getReference("comments/$contentId")

        commentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                commentsList.clear()
                for (commentSnapshot in snapshot.children) {
                    val comment = commentSnapshot.getValue(Comment::class.java)
                    comment?.let { commentsList.add(it) }
                }
                commentsAdapter.notifyDataSetChanged()

                showLoading(false)
                tvEmptyState.visibility = if (commentsList.isEmpty()) View.VISIBLE else View.GONE

                rvComments.scrollToPosition(commentsList.size - 1)
            }

            override fun onCancelled(error: DatabaseError) {
                showLoading(false)
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
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al enviar comentario: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        // No ocultamos el RecyclerView para mantener el fondo visible
    }

    data class Comment(
        val commentId: String? = null,
        val senderId: String? = null,
        val senderName: String? = null,
        val senderPhoto: String? = null,
        val content: String? = null,
        val timestamp: Long? = null
    )

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

            comment.senderPhoto?.let { photoUrl ->
                Picasso.get()
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .transform(CircleTransform())
                    .into(holder.ivProfile)
            } ?: holder.ivProfile.setImageResource(R.drawable.ic_default_profile)

            //funcionalidad para ver el perfil de todos los usuarios
            holder.ivProfile.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, ProfileActivity::class.java)
                intent.putExtra("senderId", comment.senderId)
                intent.putExtra("senderName", comment.senderName)
                intent.putExtra("senderPhoto", comment.senderPhoto)

                context.startActivity(intent)
            }
        }

        override fun getItemCount(): Int = comments.size

        private fun formatTimestamp(timestamp: Long): String {
            val diff = System.currentTimeMillis() - timestamp
            return when {
                diff < 60_000 -> "hace un momento"
                diff < 3_600_000 -> "${diff / 60_000} minutos atrás"
                diff < 86_400_000 -> "${diff / 3_600_000} horas atrás"
                else -> "${diff / 86_400_000} días atrás"
            }
        }
    }

}
