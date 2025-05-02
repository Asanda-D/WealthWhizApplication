package vcmsa.projects.wealthwhizap

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class GoalActivity : AppCompatActivity(), GoalAdapter.OnGoalClickListener {

    private lateinit var spinnerMonth: Spinner
    private lateinit var editTextMinGoal: EditText
    private lateinit var editTextMaxGoal: EditText
    private lateinit var buttonSave: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var goalAdapter: GoalAdapter
    private lateinit var db: AppDatabase
    private lateinit var instructionsText: TextView
    private lateinit var editTextBudget: EditText
    private lateinit var textViewCurrentYear: TextView

    private val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    // South African Rand currency formatter
    private val randFormat: NumberFormat = NumberFormat.getCurrencyInstance().apply {
        maximumFractionDigits = 2
        currency = java.util.Currency.getInstance("ZAR")
    }

    private fun getCurrentMonthYear(): String {
        val calendar = Calendar.getInstance()
        val month = months[calendar.get(Calendar.MONTH)]
        val year = calendar.get(Calendar.YEAR)
        return "$month $year"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal)

        // Initialize views
        spinnerMonth = findViewById(R.id.spinnerMonth)
        editTextMinGoal = findViewById(R.id.editTextMinGoal)
        editTextMaxGoal = findViewById(R.id.editTextMaxGoal)
        buttonSave = findViewById(R.id.buttonSave)
        recyclerView = findViewById(R.id.recyclerViewGoals)
        instructionsText = findViewById(R.id.textInstructions)
        editTextBudget = findViewById(R.id.editTextBudget)
        textViewCurrentYear = findViewById(R.id.textViewCurrentYear)

        // Set current year
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        textViewCurrentYear.text = "Year: $currentYear"

        // Set instructions text
        instructionsText.text = "Tap a goal to edit | Long press to delete"

        // Initialize Room Database
        db = AppDatabase.getDatabase(applicationContext)

        // Set up the spinner with months
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMonth.adapter = adapter

        // Set up RecyclerView with click listener
        goalAdapter = GoalAdapter(emptyList())
        goalAdapter.setOnGoalClickListener(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = goalAdapter

        // Save button functionality
        buttonSave.setOnClickListener {
            saveGoals()
        }

        // Observe goals changes
        val userId = getCurrentUserUsername()
        db.goalDao().getGoalsForUser(userId).observe(this) { goals ->
            goalAdapter.updateGoals(goals)
        }
    }

    override fun onGoalClick(goal: Goal) {
        // When a goal is clicked, load it into the form for editing
        val monthName = goal.month.split(" ")[0] // Extract just the month name
        val monthIndex = months.indexOf(monthName)
        if (monthIndex != -1) {
            spinnerMonth.setSelection(monthIndex)
        }
        editTextMinGoal.setText(goal.minGoal.toString())
        editTextMaxGoal.setText(goal.maxGoal.toString())
        editTextBudget.setText(goal.monthlyBudget.toString())

        Toast.makeText(this, "Editing ${goal.month}'s goal", Toast.LENGTH_SHORT).show()
    }

    override fun onGoalDelete(goal: Goal) {
        AlertDialog.Builder(this)
            .setTitle("Delete Goal")
            .setMessage("Delete the budget goal for ${goal.month}?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    db.goalDao().deleteGoal(goal)
                    Toast.makeText(
                        this@GoalActivity,
                        "${goal.month} goal deleted",
                        Toast.LENGTH_SHORT
                    ).show()
                    clearForm()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveGoals() {
        val monthName = spinnerMonth.selectedItem.toString()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val month = "$monthName $currentYear"
        val minGoal = editTextMinGoal.text.toString().toDoubleOrNull() ?: 0.0
        val maxGoal = editTextMaxGoal.text.toString().toDoubleOrNull() ?: 0.0
        val monthlyBudget = editTextBudget.text.toString().toDoubleOrNull() ?: 0.0
        val userId = getCurrentUserUsername()

        if (month.isNotEmpty() && minGoal > 0 && maxGoal > 0 && monthlyBudget > 0) {
            if (maxGoal < minGoal) {
                Toast.makeText(this, "Max goal must be greater than min", Toast.LENGTH_SHORT).show()
                return
            }
            if (maxGoal > monthlyBudget || minGoal > monthlyBudget) {
                Toast.makeText(this, "Goals cannot exceed the monthly budget", Toast.LENGTH_SHORT)
                    .show()
                return
            }

            lifecycleScope.launch {
                val existingGoal = db.goalDao().getGoalByMonthForUser(userId, month)

                if (existingGoal == null) {
                    val newGoal = Goal(
                        userId = userId,
                        month = month,
                        minGoal = minGoal,
                        maxGoal = maxGoal,
                        monthlyBudget = monthlyBudget
                    )
                    db.goalDao().insertGoal(newGoal)
                    Toast.makeText(
                        this@GoalActivity,
                        "Goal saved for $month",
                        Toast.LENGTH_SHORT
                    ).show()
                    clearForm()
                } else {
                    val updatedGoal = existingGoal.copy(
                        id = existingGoal.id, // Preserve the ID
                        minGoal = minGoal,
                        maxGoal = maxGoal,
                        monthlyBudget = monthlyBudget
                    )
                    db.goalDao().updateGoal(updatedGoal)
                    Toast.makeText(this@GoalActivity, "$month goal updated", Toast.LENGTH_SHORT)
                        .show()
                    clearForm()
                }
            }
        } else {
            Toast.makeText(this, "Please fill in all fields with valid numbers", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun clearForm() {
        editTextMinGoal.text.clear()
        editTextMaxGoal.text.clear()
        editTextBudget.text.clear()
        spinnerMonth.setSelection(0)
    }

    private fun getCurrentUserUsername(): String {
        val sharedPreferences = getSharedPreferences("WealthWhizPrefs", MODE_PRIVATE)
        val username = sharedPreferences.getString("loggedInUsername", "")
        if (username.isNullOrEmpty()) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return ""
        }
        return username
    }
}