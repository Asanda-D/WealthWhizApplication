package vcmsa.projects.wealthwhizap

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class ExpenseDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppDatabase
    private lateinit var expenseDao: ExpenseDao

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()  // Use Robolectric for test context
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        expenseDao = db.expenseDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndRetrieveExpense() = runBlocking {
        // Insert a valid CategoryEntity first to satisfy the foreign key constraint
        val category = CategoryEntity(
            id = 1, // We manually set this so that expense.categoryId = 1 will work
            userId = "testuser", // userId must match expense.username
            name = "Food",
            iconResId = 0, // Use 0 or any test value
            backgroundColor = "#FFFFFF", // dummy color
            budget = 200.0,
            subcategory = "Groceries",
            createdAt = System.currentTimeMillis()
        )

        db.categoryDao().insertCategory(category)

        val expense = Expense(
            username = "testuser",
            amount = 99.99,
            categoryId = 1,
            subCategory = "Groceries",
            dateTime = "2025-05-01 18:00",
            notes = "Bought fruits",
            imageUri = null
        )

        expenseDao.insertExpense(expense)

        val result = expenseDao.getAllExpenses("testuser").first()
        assertEquals(1, result.size)
        assertEquals("Groceries", result[0].subCategory)
        assertEquals(99.99, result[0].amount, 0.01)
    }
}
