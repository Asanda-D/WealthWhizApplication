package vcmsa.projects.wealthwhizap

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val username: String, // NEW: Username to link expense to a user
    val amount: Double,
    val categoryId: Int, // Reference to the category
    val subCategory: String?,
    val dateTime: String,
    val notes: String?,
    val imageUri: String?
)
