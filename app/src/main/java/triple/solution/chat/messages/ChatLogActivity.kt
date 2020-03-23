package triple.solution.chat.messages

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_to_row.view.*
import triple.solution.chat.R
import triple.solution.chat.models.ChatMessage
import triple.solution.chat.models.User

class ChatLogActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ChatLog"
        private const val REFERENCE_MESSAGE = "/messages/"
    }

    private val adapter = GroupAdapter<GroupieViewHolder>()
    private var toUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        toUser = intent.getParcelableExtra(NewMessageActivity.USER_KEY)
        supportActionBar?.title = toUser?.userName

        send_button_chat_log.setOnClickListener {
            Log.d(TAG, "Attempt to send message")

            sendMessage()
        }
        recyclerview_chat_log.adapter = this.adapter
        listenForMessages()
    }

    private fun listenForMessages() {
        val fromId = FirebaseAuth.getInstance().uid
        val toId = toUser?.uid
        val dataBase =
            FirebaseDatabase.getInstance()
                .getReference("$REFERENCE_MESSAGE$fromId/$toId")

        dataBase.addChildEventListener(object: ChildEventListener{

            override fun onChildAdded(snapshot: DataSnapshot, p1: String?) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java)

                if (chatMessage != null) {
                    Log.d(TAG, chatMessage.text)

                    if (chatMessage.fromId == FirebaseAuth.getInstance().uid) {
                        val currentUser = LatestMessageActivity.currentUser ?: return
                        adapter.add(ChatFromItem(chatMessage.text, currentUser))
                    } else {
                        adapter.add(ChatToItem(chatMessage.text, toUser!!))
                    }
                }

                recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)
            }

            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {

            }

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {

            }

            override fun onChildRemoved(p0: DataSnapshot) {

            }

        })
    }

    private fun sendMessage() {
        val message = editText_chat_log.text.toString()

        val user = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        val fromId = FirebaseAuth.getInstance().uid
        val toId = user.uid

        if (fromId == null){
            return
        }

        val dataBase =
            FirebaseDatabase.getInstance()
                .getReference("$REFERENCE_MESSAGE$fromId/$toId").push()

        val toDataBase =
            FirebaseDatabase.getInstance()
                .getReference("$REFERENCE_MESSAGE$toId/$fromId").push()

        val chatMessage =
            ChatMessage(dataBase.key!!, message, fromId, toId,
                System.currentTimeMillis() / 1000)

        dataBase.setValue(chatMessage)
            .addOnSuccessListener {
                Log.d(TAG, "Saved our chat message ${dataBase.key}")
                editText_chat_log.text.clear()
                recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)
            }

        toDataBase.setValue(chatMessage)

        val latestMessageFromRef = FirebaseDatabase.getInstance()
            .getReference("/latest-messages/$fromId/$toId")
        latestMessageFromRef.setValue(chatMessage)

        val latestMessageToRef = FirebaseDatabase.getInstance()
            .getReference("/latest-messages/$toId/$fromId")
        latestMessageToRef.setValue(chatMessage)
    }


}

class ChatFromItem(val text: String, private val user: User) : Item<GroupieViewHolder>() {
    override fun getLayout(): Int {
        return R.layout.chat_from_row
    }

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.textView_from_row.text = text

        val uri = user.photoProfileUrl
        Picasso.get().load(uri)
            .into(viewHolder.itemView.imageView_from_row)
    }
}

class ChatToItem(val text: String, private val user: User) : Item<GroupieViewHolder>() {
    override fun getLayout(): Int {
        return R.layout.chat_to_row
    }

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.textView_to_row.text = text

        val uri = user.photoProfileUrl
        Picasso.get().load(uri)
            .into(viewHolder.itemView.imageView_to_row)
    }
}
