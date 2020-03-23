package triple.solution.chat.messages

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.activity_latest_message.*
import kotlinx.android.synthetic.main.latest_message_row.view.*
import triple.solution.chat.R
import triple.solution.chat.models.ChatMessage
import triple.solution.chat.models.User
import triple.solution.chat.register.RegisterActivity

class LatestMessageActivity : AppCompatActivity() {

    private val adapter = GroupAdapter<GroupieViewHolder>()
    private val latestMessageMap = HashMap<String, ChatMessage>()

    companion object {
        var currentUser: User? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_latest_message)

        eventRecyclerView()
        verifyUserLogIn()
        fetchCurrentUser()
        latestMessages()
    }

    private fun eventRecyclerView() {
        recyclerView_latestMessage.adapter = adapter
        recyclerView_latestMessage.addItemDecoration(DividerItemDecoration(this,
            DividerItemDecoration.VERTICAL))

        adapter.setOnItemClickListener { item, view ->
            val intent = Intent(this, ChatLogActivity::class.java)
            val row = item as LatestMessageRow

            intent.putExtra(NewMessageActivity.USER_KEY, row.chatPartnerUser)
            startActivity(intent)
        }
    }

    private fun refreshRecyclerMessages() {
        adapter.clear()
        latestMessageMap.values.forEach {
            adapter.add(LatestMessageRow(it))
        }
    }

    private fun latestMessages() {
        val fromId = FirebaseAuth.getInstance().uid
        val dataBase =
            FirebaseDatabase.getInstance().getReference("/latest-messages/$fromId")

        dataBase.addChildEventListener(object: ChildEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {
            }

            override fun onChildChanged(snapshot: DataSnapshot, p1: String?) {
                val chatMessage =
                    snapshot.getValue(ChatMessage::class.java) ?: return
                latestMessageMap[snapshot.key!!] = chatMessage
                refreshRecyclerMessages()
            }

            override fun onChildAdded(snapshot: DataSnapshot, p1: String?) {
                val chatMessage =
                    snapshot.getValue(ChatMessage::class.java) ?: return
                latestMessageMap[snapshot.key!!] = chatMessage
                refreshRecyclerMessages()
            }

            override fun onChildRemoved(p0: DataSnapshot) {
            }
        })
    }

    private fun fetchCurrentUser() {
        val uid = FirebaseAuth.getInstance().uid
        val dataBase = FirebaseDatabase
            .getInstance().getReference("/users/$uid")

        dataBase.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(snapshot: DataSnapshot) {
                currentUser = snapshot.getValue(User::class.java)
                Log.d("LatestMessage", "Current user ${currentUser?.userName}")
            }

        })
    }

    private fun verifyUserLogIn() {
        val uid = FirebaseAuth.getInstance().uid

        if (uid == null) {
            val intent = Intent(this, RegisterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nav_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when (item?.itemId) {
            R.id.menu_new_message -> {
                val intent = Intent(this, NewMessageActivity::class.java)
                startActivity(intent)
            }
            R.id.menu_sign_out -> {
                FirebaseAuth.getInstance().signOut()
                verifyUserLogIn()
            }
        }

        return super.onOptionsItemSelected(item)
    }
}

class LatestMessageRow(private val chatMessage: ChatMessage) : Item<GroupieViewHolder>() {
    var chatPartnerUser: User? = null
    override fun getLayout(): Int {
        return R.layout.latest_message_row
    }

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.message_textView_latestMessage.text = chatMessage.text

        var chatPartnerId = chatMessage.fromId
        if (chatMessage.fromId == FirebaseAuth.getInstance().uid) {
            chatPartnerId = chatMessage.toId
        }

        val dataBase = FirebaseDatabase.getInstance()
            .getReference("/users/$chatPartnerId")

        dataBase.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                chatPartnerUser = dataSnapshot.getValue(User::class.java)
                viewHolder.itemView.username_textView_latestMessage.text =
                    chatPartnerUser?.userName

                Picasso.get().load(chatPartnerUser?.photoProfileUrl)
                    .into(viewHolder.itemView.profile_imageView_latestMessage)
            }

        })
    }

}
