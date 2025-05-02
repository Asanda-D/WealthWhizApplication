package vcmsa.projects.wealthwhizap

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firstName: String,
    val email: String,
    val username: String,
    val password: String
)
