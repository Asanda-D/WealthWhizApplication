package vcmsa.projects.wealthwhizap

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {
            lifecycleScope.launch {
                val username = etUsername.text.toString()
                val password = etPassword.text.toString()

                if (username.isBlank() || password.isBlank()) {
                    Toast.makeText(this@LoginActivity, "Please enter all fields", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val db = AppDatabase.getDatabase(this@LoginActivity)
                val user = db.userDao().login(username, password)

                if (user != null) {
                    // ✅ Save username in SharedPreferences
                    val sharedPref = getSharedPreferences("WealthWhizPrefs", Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putString("loggedInUsername", username)
                        apply()
                    }

                    Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

