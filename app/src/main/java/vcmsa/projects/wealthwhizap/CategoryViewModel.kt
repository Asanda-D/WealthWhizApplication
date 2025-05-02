package vcmsa.projects.wealthwhizap

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CategoryRepository
    private val _currentUserId = MutableLiveData<String?>()
    private val _errorMessage = MutableLiveData<String>()

    val errorMessage: LiveData<String> = _errorMessage

    init {
        val categoryDao = AppDatabase.getDatabase(application).categoryDao()
        repository = CategoryRepository(categoryDao)
    }

    fun setCurrentUserId(userId: String) {
        _currentUserId.value = userId
    }

    fun getAllCategories(): LiveData<List<CategoryEntity>> {
        val userId = _currentUserId.value
        return if (userId != null) {
            repository.getAllCategories(userId)
        } else {
            _errorMessage.value = "User ID not set"
            MutableLiveData(emptyList())
        }
    }

    fun searchCategories(query: String): LiveData<List<CategoryEntity>> {
        val userId = _currentUserId.value
        return if (userId != null) {
            repository.searchCategories(userId, "%$query%")
        } else {
            _errorMessage.value = "User ID not set"
            MutableLiveData(emptyList())
        }
    }

    fun getCategoryById(categoryId: Int): LiveData<CategoryEntity?> {
        val userId = _currentUserId.value
        return if (userId != null) {
            repository.getCategoryById(userId, categoryId)
        } else {
            _errorMessage.value = "User ID not set"
            MutableLiveData(null)
        }
    }

    fun insert(category: CategoryEntity) = viewModelScope.launch(Dispatchers.IO) {
        try {
            repository.insert(category)
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _errorMessage.value = "Failed to insert category: ${e.message}"
            }
        }
    }

    fun update(category: CategoryEntity) = viewModelScope.launch(Dispatchers.IO) {
        try {
            repository.update(category)
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _errorMessage.value = "Failed to update category: ${e.message}"
            }
        }
    }

    fun delete(category: CategoryEntity) = viewModelScope.launch(Dispatchers.IO) {
        try {
            repository.delete(category)
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _errorMessage.value = "Failed to delete category: ${e.message}"
            }
        }
    }

    fun deleteAllCategoriesForUser() = viewModelScope.launch(Dispatchers.IO) {
        val userId = _currentUserId.value
        if (userId != null) {
            try {
                repository.deleteAllCategoriesForUser(userId)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Failed to delete categories: ${e.message}"
                }
            }
        }
    }
}