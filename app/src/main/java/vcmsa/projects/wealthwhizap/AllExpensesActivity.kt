package vcmsa.projects.wealthwhizap

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AllExpensesActivity : AppCompatActivity(), ExpenseAdapter.OnImageClickListener,
    ExpenseAdapter.OnItemClickListener {

    private lateinit var tvMonth: TextView
    private lateinit var btnLast7Days: Button
    private lateinit var btnSortByCost: Button
    private lateinit var btnPreviousMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private lateinit var rvExpenses: RecyclerView
    private lateinit var btnClose: ImageButton

    private var expensesList: List<Expense> = emptyList()
    private var categoriesMap: Map<Int, CategoryEntity> = emptyMap()
    private lateinit var expenseAdapter: ExpenseAdapter
    private var selectedMonth: String = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_expenses)

        tvMonth = findViewById(R.id.tvMonth)
        btnLast7Days = findViewById(R.id.btnLast7Days)
        btnSortByCost = findViewById(R.id.btnSortByCost)
        btnPreviousMonth = findViewById(R.id.btnPreviousMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        rvExpenses = findViewById(R.id.rvExpenses)
        btnClose = findViewById(R.id.btnClose)

        btnClose.setOnClickListener {
            finish()
        }

        tvMonth.text = selectedMonth

        rvExpenses.layoutManager = LinearLayoutManager(this)
        expenseAdapter = ExpenseAdapter(this, expensesList, categoriesMap.toMutableMap())
        expenseAdapter.setOnImageClickListener(this)
        expenseAdapter.setOnItemClickListener(this)
        rvExpenses.adapter = expenseAdapter

        loadCategories()
        fetchExpensesForMonth(selectedMonth)

        btnLast7Days.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val last7Days = dateFormat.format(calendar.time)
            filterExpensesByDate(last7Days)
        }

        btnSortByCost.setOnClickListener {
            sortExpensesByCost()
        }

        btnPreviousMonth.setOnClickListener {
            navigateMonth(-1)
        }

        btnNextMonth.setOnClickListener {
            navigateMonth(1)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning from other activities
        loadCategories()
        fetchExpensesForMonth(selectedMonth)
    }

    private fun loadCategories() {
        val username = getCurrentUserUsername()
        AppDatabase.getDatabase(applicationContext)
            .categoryDao()
            .getAllCategories(username)
            .observe(this) { categories: List<CategoryEntity> ->
                categoriesMap = categories.associateBy { it.id }
                expenseAdapter.updateCategories(categoriesMap)

                // Verify all expenses have valid categories
                val invalidExpenses = expensesList.filter { expense ->
                    !categoriesMap.containsKey(expense.categoryId)
                }

                if (invalidExpenses.isNotEmpty()) {
                    lifecycleScope.launch {
                        val defaultCategory = categories.firstOrNull { it.name == "Uncategorized" }
                        if (defaultCategory != null) {
                            invalidExpenses.forEach { expense ->
                                val updatedExpense = expense.copy(categoryId = defaultCategory.id)
                                AppDatabase.getDatabase(applicationContext)
                                    .expenseDao()
                                    .updateExpense(updatedExpense)
                            }
                            fetchExpensesForMonth(selectedMonth)
                        }
                    }
                }
            }
    }

    override fun onImageClick(imageUri: String) {
        val intent = Intent(this, FullscreenImageActivity::class.java)
        intent.putExtra("imageUri", imageUri)
        startActivity(intent)
    }

    override fun onEditClick(expense: Expense) {
        val intent = Intent(this, AddExpenseActivity::class.java).apply {
            putExtra("EDIT_MODE", true)
            putExtra("EXPENSE_ID", expense.id)
        }
        startActivity(intent)
    }

    override fun onDeleteClick(expense: Expense) {
        AlertDialog.Builder(this)
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete this expense?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        AppDatabase.getDatabase(applicationContext).expenseDao().deleteExpense(expense)
                        Toast.makeText(applicationContext, "Expense deleted", Toast.LENGTH_SHORT).show()
                        fetchExpensesForMonth(selectedMonth)
                    } catch (e: Exception) {
                        Toast.makeText(applicationContext, "Error deleting expense", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateMonth(monthDelta: Int) {
        val calendar = Calendar.getInstance()
        calendar.time = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).parse(selectedMonth) ?: return
        calendar.add(Calendar.MONTH, monthDelta)
        selectedMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
        tvMonth.text = selectedMonth
        fetchExpensesForMonth(selectedMonth)
    }

    private fun fetchExpensesForMonth(month: String) {
        lifecycleScope.launch {
            val username = getCurrentUserUsername()
            AppDatabase.getDatabase(applicationContext)
                .expenseDao()
                .getAllExpenses(username)
                .collect { expenses: List<Expense> ->
                    expensesList = expenses.filter { expense ->
                        val expenseDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(expense.dateTime) ?: return@filter false
                        val expenseMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(expenseDate)
                        expenseMonth == month
                    }
                    expenseAdapter.updateExpenses(expensesList)
                }
        }
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

    private fun filterExpensesByDate(last7Days: String) {
        lifecycleScope.launch {
            val username = getCurrentUserUsername()
            AppDatabase.getDatabase(applicationContext)
                .expenseDao()
                .getAllExpenses(username)
                .collect { expenses: List<Expense> ->
                    expensesList = expenses.filter { expense ->
                        val expenseDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(expense.dateTime) ?: return@filter false
                        val last7DaysDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(last7Days) ?: return@filter false
                        expenseDate >= last7DaysDate
                    }
                    expenseAdapter.updateExpenses(expensesList)
                }
        }
    }

    private fun sortExpensesByCost() {
        expensesList = expensesList.sortedByDescending { it.amount }
        expenseAdapter.updateExpenses(expensesList)
    }
}