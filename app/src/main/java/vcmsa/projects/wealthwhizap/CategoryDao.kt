package vcmsa.projects.wealthwhizap

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * Data Access Object for CategoryEntity.
 * Provides methods to interact with the categories table in the database.
 */
@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY name ASC")
    fun getAllCategories(userId: String): LiveData<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE userId = :userId AND name LIKE :searchQuery ORDER BY name ASC")
    fun searchCategories(userId: String, searchQuery: String): LiveData<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE userId = :userId AND id = :categoryId")
    fun getCategoryById(userId: String, categoryId: Int): LiveData<CategoryEntity?>

    @Query("DELETE FROM categories WHERE userId = :userId")
    suspend fun deleteAllCategoriesForUser(userId: String)
}