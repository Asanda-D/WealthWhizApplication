package vcmsa.projects.wealthwhizap

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


class DashboardActivity : AppCompatActivity() {

    private lateinit var imgProfile: ImageView
    private lateinit var imgCoin: ImageView
    private lateinit var imgSettings: ImageView
    private lateinit var txtUsername: TextView
    private lateinit var txtCoins: TextView
    private lateinit var txtMonthlyBudget: TextView
    private lateinit var txtLeftToSpend: TextView
    private lateinit var txtBudgetStatus: TextView
    private lateinit var txtBudgetStatusValue: TextView
    private lateinit var progressBarBudget: ProgressBar
    private lateinit var textViewProgress: TextView

    private lateinit var imgCategories: ImageView
    private lateinit var imgSetGoal: ImageView
    private lateinit var imgBreakdown: ImageView
    private lateinit var imgExpenses: ImageView
    private lateinit var imgUser: ImageView
    private lateinit var imgAddExpense: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Top bar
        imgProfile = findViewById(R.id.image)
        txtUsername = findViewById(R.id.user)
        imgCoin = findViewById(R.id.imageView7)
        txtCoins = findViewById(R.id.textViewCoins)
        imgSettings = findViewById(R.id.imageView9)

        // Budget fields
        txtMonthlyBudget = findViewById(R.id.textViewMonthlyBudget)
        txtLeftToSpend = findViewById(R.id.textViewLeftToSpend)
        txtBudgetStatus = findViewById(R.id.budget_status_title)
        txtBudgetStatusValue = findViewById(R.id.budget_status_value)
        progressBarBudget = findViewById(R.id.progressBarBudget)
        textViewProgress = findViewById(R.id.textViewProgress)

        // Bottom navigation icons
        imgCategories = findViewById(R.id.imageView11)
        imgSetGoal = findViewById(R.id.imageView12)
        imgBreakdown = findViewById(R.id.imageView13)
        imgExpenses = findViewById(R.id.imageView14)
        imgUser = findViewById(R.id.imageView15)
        imgAddExpense = findViewById(R.id.imageView)

        // Set username from SharedPreferences
        val sharedPref = getSharedPreferences("WealthWhizPrefs", MODE_PRIVATE)
        val username = sharedPref.getString("loggedInUsername", "")

        if (username.isNullOrEmpty()) {
            // If no username is found, redirect to login
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        txtUsername.text = username

        // Update budget and expenses
        updateBudgetAndExpenses()

        // Setup click listeners
        imgCategories.setOnClickListener {
            startActivity(Intent(this, ManageCategoriesActivity::class.java))
        }
        imgSetGoal.setOnClickListener {
            startActivity(Intent(this, GoalActivity::class.java))
        }
        imgBreakdown.setOnClickListener { showToast("Breakdown clicked") }
        imgExpenses.setOnClickListener {
            startActivity(Intent(this, AllExpensesActivity::class.java))
        }
        imgUser.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        imgAddExpense.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateBudgetAndExpenses()
    }

    private fun updateBudgetAndExpenses() {
        lifecycleScope.launch {
            try {
                val username = getCurrentUserUsername()
                val currentMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())

                // Debug logging
                println("DEBUG: Current month: $currentMonth")
                println("DEBUG: Username: $username")

                // Get current month's budget
                val goal = AppDatabase.getDatabase(applicationContext).goalDao().getGoalByMonthForUser(username, currentMonth)
                println("DEBUG: Found goal: $goal")
                val monthlyBudget = goal?.monthlyBudget ?: 0.0
                println("DEBUG: Monthly budget: $monthlyBudget")

                // Get current month's expenses using Flow
                AppDatabase.getDatabase(applicationContext)
                    .expenseDao()
                    .getAllExpenses(username)
                    .collect { expenses ->
                        println("DEBUG: Total expenses found: ${expenses.size}")
                        val currentMonthExpenses = expenses.filter {
                            try {
                                val expenseDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.dateTime) ?: return@filter false
                                val expenseMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(expenseDate)
                                expenseMonth == currentMonth
                            } catch (e: Exception) {
                                println("DEBUG: Error parsing date: ${it.dateTime}")
                                false // Skip expenses with invalid date format
                            }
                        }
                        println("DEBUG: Current month expenses: ${currentMonthExpenses.size}")

                        val totalExpenses = currentMonthExpenses.sumOf { it.amount }
                        val remainingBudget = monthlyBudget - totalExpenses

                        // Calculate spending percentage
                        val spendingPercentage = if (monthlyBudget > 0) {
                            ((totalExpenses / monthlyBudget) * 100).toInt()
                        } else {
                            0
                        }

                        // Update UI on main thread
                        runOnUiThread {
                            if (monthlyBudget == 0.0) {
                                println("DEBUG: No budget set for current month")
                                txtMonthlyBudget.text = "No Budget Set"
                                txtLeftToSpend.text = "Set a Budget"
                                txtBudgetStatusValue.text = "Not Set"
                                txtBudgetStatusValue.setTextColor(resources.getColor(android.R.color.darker_gray, theme))
                                progressBarBudget.progress = 0
                                textViewProgress.text = "0% spent"
                            } else {
                                println("DEBUG: Setting budget display: R$monthlyBudget")
                                txtMonthlyBudget.text = "R${String.format("%.2f", monthlyBudget)}"
                                txtLeftToSpend.text = "R${String.format("%.2f", remainingBudget)}"

                                // Update progress bar
                                progressBarBudget.progress = spendingPercentage.coerceAtMost(100)
                                textViewProgress.text = "$spendingPercentage% spent"

                                // Update budget status with enhanced messages
                                when {
                                    remainingBudget < 0 -> {
                                        txtBudgetStatusValue.text = "Over Budget!"
                                        txtBudgetStatusValue.setTextColor(resources.getColor(R.color.budget_over, theme))
                                    }
                                    spendingPercentage >= 80 -> {
                                        txtBudgetStatusValue.text = "Warning: ${100 - spendingPercentage}% left"
                                        txtBudgetStatusValue.setTextColor(resources.getColor(R.color.budget_warning, theme))
                                    }
                                    else -> {
                                        txtBudgetStatusValue.text = "Good: ${100 - spendingPercentage}% left"
                                        txtBudgetStatusValue.setTextColor(resources.getColor(R.color.budget_good, theme))
                                    }
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                println("DEBUG: Error in updateBudgetAndExpenses: ${e.message}")
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@DashboardActivity, "Error updating budget: ${e.message}", Toast.LENGTH_SHORT).show()
                    txtMonthlyBudget.text = "Error"
                    txtLeftToSpend.text = "Error"
                    txtBudgetStatusValue.text = "Error"
                }
            }
        }
    }

    private fun getCurrentUserUsername(): String {
        val sharedPref = getSharedPreferences("WealthWhizPrefs", MODE_PRIVATE)
        return sharedPref.getString("loggedInUsername", "") ?: ""
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
