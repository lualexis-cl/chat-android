package triple.solution.chat.register

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_register.*
import triple.solution.chat.messages.LatestMessageActivity
import triple.solution.chat.R
import triple.solution.chat.models.User
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private var selectImageUri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        account_textView.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        register_button.setOnClickListener {
            val email = email_editText_register.text.toString()
            val password = password_editText_register.text.toString()

            createUser(email, password)
        }

        photo_button_register.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && resultCode == Activity.RESULT_OK &&
                data != null) {
            Log.d("RegisterImage", "Photo was selected")
            selectImageUri = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectImageUri)
            select_photo_register.setImageBitmap(bitmap)
            photo_button_register.alpha = 0f
        }
    }

    private fun createUser(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter text/password", Toast.LENGTH_LONG)
                .show()
            return
        }

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful) {
                    return@addOnCompleteListener
                }

                Log.d("Main", "user succesfully created with uid: ${it.result!!.user!!.uid}")
                registerImage()
            }
            .addOnFailureListener {
                Log.d("Main", "Failed to create user: ${it.message}")

                Toast.makeText(this, "Failed to create user: ${it.message}", Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun registerImage() {
        if (selectImageUri == null) {
            return
        }

        val fileName = UUID.randomUUID().toString()
        val storage = FirebaseStorage.getInstance()
            .getReference("/images/$fileName")

        storage.putFile(selectImageUri!!)
            .addOnSuccessListener {
                Log.d("RegisterImage", "Succesfully uploaded image: ${it.metadata?.path}")

                storage.downloadUrl.addOnSuccessListener { result ->
                    Log.d("RegisterImage", "File Location $result")

                    registerUser(result.toString())
                }
            }
    }

    private fun registerUser(photoProfileUrl: String) {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val dataBase = FirebaseDatabase.getInstance().getReference("/users/$uid")
        val userName = userName_editText_register.text.toString()

        val user = User(uid, userName, photoProfileUrl)

        dataBase.setValue(user)
            .addOnSuccessListener {
                Log.d("RegisterUser", "Finally we saved the user to Firebase")

                val intent = Intent(this, LatestMessageActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
    }
}