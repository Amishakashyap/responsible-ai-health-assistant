package com.example.foodtracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface FoodDao {
    @Query("SELECT * FROM food WHERE id = :id")
    suspend fun getById(id: Long): Food?

    @Query("SELECT * FROM food WHERE name_normalized LIKE :q LIMIT 50")
    suspend fun searchLike(q: String): List<Food>

    @RawQuery
    suspend fun searchFts(query: SupportSQLiteQuery): List<Food>

    suspend fun searchFts(term: String): List<Food> {
        val sql = """
            SELECT f.* FROM food f
            JOIN food_fts ft ON f.id = ft.rowid
            WHERE ft MATCH ?
            LIMIT 50
        """.trimIndent()
        val q = SimpleSQLiteQuery(sql, arrayOf("${term.trim()}*"))
        return searchFts(q)
    }
}

@Dao
interface EntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: Entry): Long

    @Query("SELECT * FROM entry WHERE date = :date")
    suspend fun getByDate(date: String): List<Entry>
    
    @Query("SELECT * FROM entry WHERE date = :date AND user_id = :userId")
    suspend fun getByDateAndUser(date: String, userId: Long): List<Entry>

    @Query("DELETE FROM entry WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User): Long

    @Query("SELECT * FROM user WHERE email = :email AND password = :password LIMIT 1")
    suspend fun login(email: String, password: String): User?

    @Query("SELECT * FROM user WHERE email = :email LIMIT 1")
    suspend fun loginByEmail(email: String): User?

    @Query("SELECT * FROM user WHERE id = :id")
    suspend fun getById(id: Long): User?
}

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfile)

    @Query("SELECT * FROM user_profile WHERE userId = :userId")
    suspend fun getByUserId(userId: Long): UserProfile?
}

@Dao
interface DailyNutritionLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: DailyNutritionLog): Long
    
    @Query("SELECT * FROM daily_nutrition_log WHERE user_id = :userId AND date = :date LIMIT 1")
    suspend fun getByUserAndDate(userId: Long, date: String): DailyNutritionLog?
    
    @Query("SELECT * FROM daily_nutrition_log WHERE user_id = :userId ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentLogs(userId: Long, limit: Int = 7): List<DailyNutritionLog>
    
    @Query("SELECT * FROM daily_nutrition_log WHERE user_id = :userId AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getLogsInRange(userId: Long, startDate: String, endDate: String): List<DailyNutritionLog>
    
    @Query("DELETE FROM daily_nutrition_log WHERE user_id = :userId AND date = :date")
    suspend fun deleteByUserAndDate(userId: Long, date: String)
}

@Dao
interface RecipeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipe: Recipe): Long
    
    @Query("SELECT * FROM recipe WHERE id = :id")
    suspend fun getById(id: Long): Recipe?
    
    @Query("SELECT * FROM recipe WHERE meal_type = :mealType")
    suspend fun getByMealType(mealType: String): List<Recipe>
    
    @Query("SELECT * FROM recipe WHERE diet_type = :dietType OR diet_type = 'omnivore'")
    suspend fun getByDietType(dietType: String): List<Recipe>
    
    @Query("SELECT * FROM recipe WHERE calories_per_serving <= :maxCalories")
    suspend fun getByMaxCalories(maxCalories: Int): List<Recipe>
    
    @Query("SELECT * FROM recipe WHERE protein_per_serving >= :minProtein")
    suspend fun getByMinProtein(minProtein: Int): List<Recipe>
    
    @Query("""
        SELECT * FROM recipe 
        WHERE meal_type = :mealType 
        AND (diet_type = :dietType OR diet_type = 'omnivore')
        AND calories_per_serving BETWEEN :minCalories AND :maxCalories
        ORDER BY RANDOM()
        LIMIT :limit
    """)
    suspend fun findSuitableRecipes(
        mealType: String,
        dietType: String,
        minCalories: Int,
        maxCalories: Int,
        limit: Int = 5
    ): List<Recipe>
    
    @Query("SELECT * FROM recipe ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomRecipes(limit: Int = 10): List<Recipe>
}

@Dao
interface DailyHealthSummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: DailyHealthSummary): Long
    
    @Query("SELECT * FROM daily_health_summary WHERE user_id = :userId AND date = :date ORDER BY generated_at DESC LIMIT 1")
    suspend fun getByUserAndDate(userId: Long, date: String): DailyHealthSummary?
    
    @Query("SELECT * FROM daily_health_summary WHERE user_id = :userId ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentSummaries(userId: Long, limit: Int = 7): List<DailyHealthSummary>
    
    @Query("DELETE FROM daily_health_summary WHERE user_id = :userId AND date = :date")
    suspend fun deleteByUserAndDate(userId: Long, date: String)
}

@Dao
interface WeightEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WeightEntry): Long

    @Query("SELECT * FROM weight_entry WHERE user_id = :userId ORDER BY recorded_at DESC")
    suspend fun getByUser(userId: Long): List<WeightEntry>

    @Query("SELECT * FROM weight_entry WHERE user_id = :userId ORDER BY recorded_at DESC LIMIT 1")
    suspend fun getLatestByUser(userId: Long): WeightEntry?

    @Query("DELETE FROM weight_entry WHERE id = :id")
    suspend fun deleteById(id: Long)
}
