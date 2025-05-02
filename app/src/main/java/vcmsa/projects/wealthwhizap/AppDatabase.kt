package vcmsa.projects.wealthwhizap

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [User::class, Expense::class, Goal::class, CategoryEntity::class],
    version = 3
    //exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun goalDao(): GoalDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d("Migration", "Migrating from version 1 to 2...")

                // Step 1: Create the categories table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        iconResId INTEGER NOT NULL,
                        backgroundColor TEXT NOT NULL,
                        budget REAL,
                        subcategory TEXT,
                        createdAt INTEGER NOT NULL
                    )
                """)

                // Step 2: Create a temporary expenses table that references the new categories table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS expenses_temp (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        username TEXT NOT NULL,
                        amount REAL NOT NULL,
                        categoryId INTEGER NOT NULL,
                        subCategory TEXT,
                        dateTime TEXT NOT NULL,
                        notes TEXT,
                        imageUri TEXT,
                        FOREIGN KEY (categoryId) REFERENCES categories(id) ON DELETE CASCADE
                    )
                """)

                // Step 3: Copy data from old expenses to new temp table (assign default categoryId = 1)
                database.execSQL("""
                    INSERT INTO expenses_temp (id, username, amount, categoryId, subCategory, dateTime, notes, imageUri)
                    SELECT id, username, amount, 1, subCategory, dateTime, notes, imageUri FROM expenses
                """)

                // Step 4: Drop old expenses table
                database.execSQL("DROP TABLE IF EXISTS expenses")

                // Step 5: Rename temp table to expenses
                database.execSQL("ALTER TABLE expenses_temp RENAME TO expenses")

                // Step 6: Create index for performance
                database.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_categoryId ON expenses(categoryId)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wealthwhiz_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration() // Only use in dev/debug to avoid crash if migration fails
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

