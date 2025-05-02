package vcmsa.projects.wealthwhizap

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var etAmount: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var etSubCategory: EditText
    private lateinit var btnPickDateTime: Button
    private lateinit var tvDateTime: TextView
    private lateinit var etNotes: EditText
    private lateinit var btnPickImage: Button
    private lateinit var imagePreview: ImageView
    private lateinit var btnSaveExpense: Button
    private lateinit var btnClose: ImageButton

    private var selectedDateTime: String = ""
    private var selectedImageUri: String? = null
    private var isEditMode = false
    private var expenseId = -1
    private var selectedCategoryId: Int = -1
    private val categories = mutableListOf<CategoryEntity>()

   // private lateinit var imagePickerLauncher: ActivityResultLauncher<String>
   private lateinit var imagePickerLauncher: ActivityResultLauncher<Array<String>>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        etAmount = findViewById(R.id.etAmount)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        etSubCategory = findViewById(R.id.etSubCategory)
        btnPickDateTime = findViewById(R.id.btnPickDateTime)
        tvDateTime = findViewById(R.id.tvDateTime)
        etNotes = findViewById(R.id.etNotes)
        btnPickImage = findViewById(R.id.btnPickImage)
        imagePreview = findViewById(R.id.imagePreview)
        btnSaveExpense = findViewById(R.id.btnSaveExpense)
        btnClose = findViewById(R.id.btnClose)

        btnClose.setOnClickListener {
            finish()
        }

        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)
        expenseId = intent.getIntExtra("EXPENSE_ID", -1)

        setupImagePicker()
        loadCategories()

        if (isEditMode) {
            loadExpenseData()
            btnSaveExpense.text = "Update Expense"
        }

        btnPickDateTime.setOnClickListener {
            showDateTimePicker()
        }

        btnPickImage.setOnClickListener {
            pickImageFromGallery()
        }

        btnSaveExpense.setOnClickListener {
            saveExpenseToDatabase()
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val username = getCurrentUsername()
                AppDatabase.getDatabase(applicationContext)
                    .categoryDao()
                    .getAllCategories(username)
                    .observe(this@AddExpenseActivity) { categoryList ->
                        categories.clear()
                        categories.addAll(categoryList)

                        if (categories.isEmpty()) {
                            // Create a default category if none exist
                            val defaultCategory = CategoryEntity(
                                userId = username,
                                name = "Uncategorized",
                                iconResId = R.drawable.ic_category_default,
                                backgroundColor = "#CCCCCC"
                            )
                            lifecycleScope.launch {
                                AppDatabase.getDatabase(applicationContext)
                                    .categoryDao()
                                    .insertCategory(defaultCategory)
                                categories.add(defaultCategory)

                                val categoryNames = categories.map { it.name }
                                val adapter = ArrayAdapter(
                                    this@AddExpenseActivity,
                                    android.R.layout.simple_spinner_dropdown_item,
                                    categoryNames
                                )
                                spinnerCategory.adapter = adapter

                                spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                                        selectedCategoryId = categories[position].id
                                    }

                                    override fun onNothingSelected(parent: AdapterView<*>?) {
                                        selectedCategoryId = -1
                                    }
                                }
                            }
                        } else {
                            val categoryNames = categories.map { it.name }
                            val adapter = ArrayAdapter(
                                this@AddExpenseActivity,
                                android.R.layout.simple_spinner_dropdown_item,
                                categoryNames
                            )
                            spinnerCategory.adapter = adapter

                            spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                                    selectedCategoryId = categories[position].id
                                }

                                override fun onNothingSelected(parent: AdapterView<*>?) {
                                    selectedCategoryId = -1
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                Toast.makeText(this@AddExpenseActivity, "Error loading categories: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                // Persist permission safely
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                selectedImageUri = it.toString()
                imagePreview.setImageURI(it)
                imagePreview.visibility = ImageView.VISIBLE
            }
        }
    }


    private fun loadExpenseData() {
        lifecycleScope.launch {
            val expense = AppDatabase.getDatabase(applicationContext)
                .expenseDao()
                .getExpenseById(expenseId)

            expense?.let {
                runOnUiThread {
                    etAmount.setText(it.amount.toString())
                    val categoryPosition = categories.indexOfFirst { category -> category.id == it.categoryId }
                    if (categoryPosition != -1) {
                        spinnerCategory.setSelection(categoryPosition)
                    }
                    etSubCategory.setText(it.subCategory ?: "")
                    tvDateTime.text = it.dateTime
                    selectedDateTime = it.dateTime
                    etNotes.setText(it.notes ?: "")

                    it.imageUri?.let { uri ->
                        selectedImageUri = uri
                        imagePreview.setImageURI(Uri.parse(uri))
                        imagePreview.visibility = ImageView.VISIBLE
                    }
                }
            }
        }
    }

    private fun pickImageFromGallery() {
        imagePickerLauncher.launch(arrayOf("image/*"))
    }


    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()

        DatePickerDialog(this, { _, year, month, day ->
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(year, month, day, hour, minute)
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                selectedDateTime = format.format(calendar.time)
                tvDateTime.text = selectedDateTime
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun saveExpenseToDatabase() {
        val amountText = etAmount.text.toString()
        if (amountText.isEmpty()) {
            Toast.makeText(this, "Amount is required", Toast.LENGTH_SHORT).show()
            return
        }
        val amount = amountText.toDoubleOrNull()
        if (amount == null) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedCategoryId == -1) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        val subCategory = etSubCategory.text.toString()

        if (selectedDateTime.isEmpty()) {
            Toast.makeText(this, "Please select a date and time", Toast.LENGTH_SHORT).show()
            return
        }

        val notes = etNotes.text.toString()
        val username = getCurrentUsername()

        val expense = if (isEditMode) {
            Expense(
                id = expenseId,
                username = username,
                amount = amount,
                categoryId = selectedCategoryId,
                subCategory = subCategory,
                dateTime = selectedDateTime,
                notes = notes,
                imageUri = selectedImageUri
            )
        } else {
            Expense(
                username = username,
                amount = amount,
                categoryId = selectedCategoryId,
                subCategory = subCategory,
                dateTime = selectedDateTime,
                notes = notes,
                imageUri = selectedImageUri
            )
        }

        lifecycleScope.launch {
            try {
                if (isEditMode) {
                    AppDatabase.getDatabase(applicationContext).expenseDao().updateExpense(expense)
                    Toast.makeText(applicationContext, "Expense updated!", Toast.LENGTH_SHORT).show()
                } else {
                    AppDatabase.getDatabase(applicationContext).expenseDao().insertExpense(expense)
                    Toast.makeText(applicationContext, "Expense saved!", Toast.LENGTH_SHORT).show()
                }
                finish()
            } catch (e: Exception) {
                Toast.makeText(applicationContext, "Error saving expense: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCurrentUsername(): String {
        val sharedPrefs = getSharedPreferences("WealthWhizPrefs", MODE_PRIVATE)
        val username = sharedPrefs.getString("loggedInUsername", "")
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









