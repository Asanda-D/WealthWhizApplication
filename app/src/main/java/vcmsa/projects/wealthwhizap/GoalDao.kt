package vcmsa.projects.wealthwhizap

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface GoalDao {

    @Insert
    suspend fun insertGoal(goal: Goal)

    @Update
    suspend fun updateGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)

    @Query("SELECT * FROM goals WHERE userId = :userId ORDER BY month DESC")
    fun getGoalsForUser(userId: String): LiveData<List<Goal>>

    @Query("SELECT * FROM goals WHERE userId = :userId AND month = :month LIMIT 1")
    suspend fun getGoalByMonthForUser(userId: String, month: String): Goal?

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: Int)

}