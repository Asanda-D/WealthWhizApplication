package vcmsa.projects.wealthwhizap

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CategoryExpensesActivity : AppCompatActivity(), ExpenseAdapter.OnImageClickListener,
    ExpenseAdapter.OnItemClickListener {

    private lateinit var tvMonth: TextView
    private lateinit var tvCategoryName: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var btnPreviousMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private lateinit var rvExpenses: RecyclerView
    private lateinit var btnClose: ImageButton
    private lateinit var sharedPreferences: SharedPreferences

    private var expensesList: List<Expense> = emptyList()
    private var categoriesMap: Map<Int, CategoryEntity> = emptyMap()
    private lateinit var expenseAdapter: ExpenseAdapter
    private var selectedMonth: String = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    private var categoryId: Int = -1
    private var categoryName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_expenses)

        sharedPreferences = getSharedPreferences("WealthWhizPrefs", MODE_PRIVATE)

        // Get category ID and name from intent
        categoryId = intent.getIntExtra("CATEGORY_ID", -1)
        categoryName = intent.getStringExtra("CATEGORY_NAME") ?: ""

        if (categoryId == -1) {
            Toast.makeText(this, "Invalid category", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvMonth = findViewById(R.id.tvMonth)
        tvCategoryName = findViewById(R.id.tvCategoryName)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        btnPreviousMonth = findViewById(R.id.btnPreviousMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        rvExpenses = findViewById(R.id.rvExpenses)
        btnClose = findViewById(R.id.btnClose)

        tvCategoryName.text = categoryName
        tvMonth.text = selectedMonth

        btnClose.setOnClickListener {
            finish()
        }

        rvExpenses.layoutManager = LinearLayoutManager(this)
        expenseAdapter = ExpenseAdapter(this, expensesList, categoriesMap.toMutableMap())
        expenseAdapter.setOnImageClickListener(this)
        expenseAdapter.setOnItemClickListener(this)
        rvExpenses.adapter = expenseAdapter

        loadCategories()
        fetchExpensesForMonth(selectedMonth)

        btnPreviousMonth.setOnClickListener {
            navigateMonth(-1)
        }

        btnNextMonth.setOnClickListener {
            navigateMonth(1)
        }
    }

    override fun onResume() {
        super.onResume()
        loadCategories()
        fetchExpensesForMonth(selectedMonth)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up any resources if needed
    }

    private fun getCurrentUserUsername(): String {
        return sharedPreferences.getString("loggedInUsername", "") ?: ""
    }

    private fun loadCategories() {
        val username = getCurrentUserUsername()
        if (username.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        AppDatabase.getDatabase(applicationContext)
            .categoryDao()
            .getAllCategories(username)
            .observe(this) { categories: List<CategoryEntity> ->
                categoriesMap = categories.associateBy { it.id }
                expenseAdapter.updateCategories(categoriesMap)
            }
    }

    private fun fetchExpensesForMonth(month: String) {
        lifecycleScope.launch {
            try {
                val username = getCurrentUserUsername()
                if (username.isEmpty()) {
                    Toast.makeText(this@CategoryExpensesActivity, "User not logged in", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                val date = dateFormat.parse(month) ?: run {
                    Toast.makeText(this@CategoryExpensesActivity, "Invalid date format", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val calendar = Calendar.getInstance()
                calendar.time = date

                val startDate = SimpleDateFormat("yyyy-MM-01", Locale.getDefault()).format(calendar.time)
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

                AppDatabase.getDatabase(applicationContext)
                    .expenseDao()
                    .getExpensesByCategoryAndDateRange(username, categoryId, startDate, endDate)
                    .collect { expenses ->
                        expensesList = expenses
                        expenseAdapter.updateExpenses(expensesList)

                        // Calculate and display total amount
                        val total = expenses.sumOf { it.amount }
                        tvTotalAmount.text = String.format("Total: R%.2f", total)
                    }
            } catch (e: Exception) {
                Toast.makeText(this@CategoryExpensesActivity, "Error loading expenses: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateMonth(direction: Int) {
        try {
            val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val date = dateFormat.parse(selectedMonth) ?: run {
                Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show()
                return
            }
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.add(Calendar.MONTH, direction)
            selectedMonth = dateFormat.format(calendar.time)
            tvMonth.text = selectedMonth
            fetchExpensesForMonth(selectedMonth)
        } catch (e: Exception) {
            Toast.makeText(this, "Error navigating months: ${e.message}", Toast.LENGTH_SHORT).show()
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
        lifecycleScope.launch {
            try {
                AppDatabase.getDatabase(applicationContext)
                    .expenseDao()
                    .deleteExpense(expense)
                fetchExpensesForMonth(selectedMonth)
            } catch (e: Exception) {
                Toast.makeText(this@CategoryExpensesActivity, "Error deleting expense: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}