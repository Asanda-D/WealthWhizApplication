package vcmsa.projects.wealthwhizap

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.io.Serializable

/**
 * Entity class representing a category in the WealthWhizApp.
 * Categories are used to organize expenses and track budgets.
 */
@Entity(tableName = "categories")
@Parcelize
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String, // ID of the user who owns this category
    val name: String,
    val iconResId: Int,
    val backgroundColor: String,
    val budget: Double? = null,
    val subcategory: String? = null,
    val createdAt: Long = System.currentTimeMillis() // Track when category was created
) : Parcelable, Serializable
