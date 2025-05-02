package vcmsa.projects.wealthwhizap

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String, // NEW: ID of the user who created this goal
    val month: String,
    val minGoal: Double,
    val maxGoal: Double,
    val monthlyBudget: Double

)