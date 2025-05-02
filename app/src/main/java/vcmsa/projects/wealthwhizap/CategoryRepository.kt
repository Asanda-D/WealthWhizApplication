package vcmsa.projects.wealthwhizap

import androidx.lifecycle.LiveData

/**
 * Repository class for handling category data operations.
 * Acts as a mediator between the ViewModel and the DAO.
 */
class CategoryRepository(private val categoryDao: CategoryDao) {

    fun getAllCategories(userId: String): LiveData<List<CategoryEntity>> {
        return categoryDao.getAllCategories(userId)
    }

    fun searchCategories(userId: String, searchQuery: String): LiveData<List<CategoryEntity>> {
        return categoryDao.searchCategories(userId, searchQuery)
    }

    fun getCategoryById(userId: String, categoryId: Int): LiveData<CategoryEntity?> {
        return categoryDao.getCategoryById(userId, categoryId)
    }

    suspend fun insert(category: CategoryEntity) {
        categoryDao.insertCategory(category)
    }

    suspend fun update(category: CategoryEntity) {
        categoryDao.updateCategory(category)
    }

    suspend fun delete(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
    }

    suspend fun deleteAllCategoriesForUser(userId: String) {
        categoryDao.deleteAllCategoriesForUser(userId)
    }
}