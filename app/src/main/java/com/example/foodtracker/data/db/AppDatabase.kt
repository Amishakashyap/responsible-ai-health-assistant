package com.example.foodtracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Database(
    entities = [
        User::class, 
        Food::class, 
        Entry::class, 
        UserProfile::class,
        DailyNutritionLog::class,
        Recipe::class,
        DailyHealthSummary::class,
        WeightEntry::class
    ],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun foodDao(): FoodDao
    abstract fun entryDao(): EntryDao
    abstract fun profileDao(): ProfileDao
    abstract fun dailyNutritionLogDao(): DailyNutritionLogDao
    abstract fun recipeDao(): RecipeDao
    abstract fun dailyHealthSummaryDao(): DailyHealthSummaryDao
    abstract fun weightEntryDao(): WeightEntryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "food.db")
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Insert sample foods synchronously so they're available immediately
                        try {
                            android.util.Log.d("AppDatabase", "Creating database, inserting sample foods")
                            insertSampleFoods(context, db)
                            android.util.Log.d("AppDatabase", "Sample foods inserted successfully")
                        } catch (e: Exception) {
                            android.util.Log.e("AppDatabase", "Error inserting sample foods", e)
                        }
                    }
                    
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // Check if food table is empty and populate if needed (synchronously)
                        try {
                            val cursor = db.query("SELECT COUNT(*) FROM food")
                            cursor.moveToFirst()
                            val count = cursor.getInt(0)
                            cursor.close()
                            
                            if (count == 0) {
                                android.util.Log.d("AppDatabase", "Food table is empty, populating with sample foods")
                                insertSampleFoods(context, db)
                            } else {
                                android.util.Log.d("AppDatabase", "Food table has $count items")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("AppDatabase", "Error checking/populating foods", e)
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
        }

        private fun insertSampleFoods(context: Context, db: SupportSQLiteDatabase) {
            android.util.Log.d("AppDatabase", "Starting to insert Indian foods from CSV")
            
            try {
                // Read CSV file from assets
                val inputStream = context.assets.open("indian_foods.csv")
                val reader = inputStream.bufferedReader()
                
                // Skip header line
                reader.readLine()
                
                var inserted = 0
                var line: String?
                
                while (reader.readLine().also { line = it } != null) {
                    try {
                        // Simple CSV parsing (fields: Dish Name, Calories, Carbs, Protein, Fats, ...)
                        val parts = line!!.split(",")
                        
                        if (parts.size >= 5) {
                            val dishName = parts[0].trim().removeSurrounding("\"")
                            val calories = parts[1].trim().toDoubleOrNull() ?: 0.0
                            val carbs = parts[2].trim().toDoubleOrNull() ?: 0.0
                            val protein = parts[3].trim().toDoubleOrNull() ?: 0.0
                            val fats = parts[4].trim().toDoubleOrNull() ?: 0.0
                            
                            // Skip invalid entries
                            if (dishName.isBlank()) continue
                            
                            // Create normalized name (lowercase, remove special chars)
                            val normalizedName = dishName.lowercase()
                                .replace(Regex("[^a-z0-9\\s]"), "")
                                .replace(Regex("\\s+"), " ")
                                .trim()
                            
                            // Insert into database (escape single quotes)
                            val safeName = dishName.replace("'", "''")
                            val safeNormName = normalizedName.replace("'", "''")
                            
                            db.execSQL("""
                                INSERT INTO food (name, name_normalized, calories_kcal_per_100g, 
                                                 protein_g_per_100g, carbs_g_per_100g, fat_g_per_100g)
                                VALUES ('$safeName', '$safeNormName', $calories, $protein, $carbs, $fats)
                            """.trimIndent())
                            inserted++
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AppDatabase", "Error parsing CSV line: $line", e)
                    }
                }
                
                reader.close()
                inputStream.close()
                
                android.util.Log.d("AppDatabase", "Successfully inserted $inserted Indian foods from CSV")
            } catch (e: Exception) {
                android.util.Log.e("AppDatabase", "Error reading CSV file, using fallback data", e)
                // Fallback to minimal sample data if CSV fails
                insertFallbackFoods(db)
            }
        }
        
        private fun insertFallbackFoods(db: SupportSQLiteDatabase) {
            android.util.Log.d("AppDatabase", "Inserting fallback foods")
            val foods = listOf(
                Triple("Poha", 158.0, Triple(2.5, 30.0, 3.0)),
                Triple("Idli", 39.0, Triple(2.0, 8.0, 0.1)),
                Triple("Dosa", 120.0, Triple(2.0, 18.0, 4.0)),
                Triple("Chapati", 71.0, Triple(2.8, 13.3, 0.4)),
                Triple("Rice", 130.0, Triple(2.7, 28.2, 0.3)),
                Triple("Dal", 135.0, Triple(8.0, 22.0, 2.5))
            )
            
            foods.forEach { (name, calories, nutrients) ->
                try {
                    val (protein, carbs, fat) = nutrients
                    db.execSQL("""
                        INSERT INTO food (name, name_normalized, calories_kcal_per_100g, protein_g_per_100g, carbs_g_per_100g, fat_g_per_100g) 
                        VALUES ('$name', '${name.lowercase()}', $calories, $protein, $carbs, $fat)
                    """.trimIndent())
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Error inserting fallback food: $name", e)
                }
            }
        }
    }
}
