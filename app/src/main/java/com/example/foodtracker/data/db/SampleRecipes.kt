package com.example.foodtracker.data.db

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Sample recipes for the AI health assistant
 * These are verified recipes with known nutrition data
 */
object SampleRecipes {
    
    /**
     * Insert sample recipes into the database
     * Call this once on app first launch or from settings
     */
    fun insertSampleRecipes(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.get(context)
            val recipeDao = db.recipeDao()
            
            // Check if recipes already exist
            val existing = recipeDao.getRandomRecipes(1)
            if (existing.isNotEmpty()) {
                return@launch // Already populated
            }
            
            // Breakfast Recipes
            recipeDao.insert(Recipe(
                name = "High-Protein Greek Yogurt Bowl",
                description = "Creamy Greek yogurt with berries, nuts, and granola",
                caloriesPerServing = 320,
                proteinPerServing = 28,
                carbsPerServing = 35,
                fatPerServing = 8,
                servings = 1,
                prepTimeMinutes = 5,
                cookTimeMinutes = 0,
                dietType = "vegetarian",
                mealType = "breakfast",
                ingredients = """
200g Greek yogurt (plain, non-fat)
1/2 cup mixed berries (blueberries, strawberries)
30g granola
15g almonds (sliced)
1 tsp honey (optional)
                """.trimIndent(),
                instructions = """
1. Add Greek yogurt to a bowl
2. Top with mixed berries
3. Sprinkle granola evenly
4. Add sliced almonds
5. Drizzle with honey if desired
6. Enjoy immediately
                """.trimIndent(),
                tags = "high-protein,quick,no-cook,vegetarian"
            ))
            
            recipeDao.insert(Recipe(
                name = "Veggie Omelette",
                description = "Fluffy 3-egg omelette loaded with vegetables",
                caloriesPerServing = 280,
                proteinPerServing = 24,
                carbsPerServing = 8,
                fatPerServing = 16,
                servings = 1,
                prepTimeMinutes = 5,
                cookTimeMinutes = 10,
                dietType = "vegetarian",
                mealType = "breakfast",
                ingredients = """
3 large eggs
1/4 cup spinach (chopped)
1/4 cup tomatoes (diced)
2 tbsp onions (diced)
1/4 cup bell peppers (diced)
Salt and pepper to taste
1 tsp olive oil
                """.trimIndent(),
                instructions = """
1. Beat eggs in a bowl with salt and pepper
2. Heat olive oil in non-stick pan over medium heat
3. Sauté vegetables for 2-3 minutes until soft
4. Pour beaten eggs over vegetables
5. Cook for 3-4 minutes until edges set
6. Fold omelette in half
7. Cook for 2 more minutes until fully cooked
8. Serve hot
                """.trimIndent(),
                tags = "high-protein,quick,vegetarian,low-carb"
            ))
            
            recipeDao.insert(Recipe(
                name = "Overnight Protein Oats",
                description = "Make-ahead oats with protein powder and chia seeds",
                caloriesPerServing = 380,
                proteinPerServing = 32,
                carbsPerServing = 45,
                fatPerServing = 9,
                servings = 1,
                prepTimeMinutes = 5,
                cookTimeMinutes = 0,
                dietType = "vegetarian",
                mealType = "breakfast",
                ingredients = """
1/2 cup rolled oats
1 scoop vanilla protein powder (25g protein)
1 cup almond milk
1 tbsp chia seeds
1/2 banana (sliced)
1 tsp honey
Cinnamon to taste
                """.trimIndent(),
                instructions = """
1. Mix oats, protein powder, and chia seeds in jar
2. Add almond milk and stir well
3. Add sliced banana and honey
4. Sprinkle cinnamon on top
5. Cover and refrigerate overnight (8 hours)
6. Stir before eating
7. Add toppings if desired (berries, nuts)
                """.trimIndent(),
                tags = "high-protein,make-ahead,vegetarian,no-cook"
            ))
            
            // Lunch Recipes
            recipeDao.insert(Recipe(
                name = "Grilled Chicken Salad",
                description = "Fresh greens with grilled chicken breast and balsamic dressing",
                caloriesPerServing = 350,
                proteinPerServing = 42,
                carbsPerServing = 15,
                fatPerServing = 12,
                servings = 1,
                prepTimeMinutes = 10,
                cookTimeMinutes = 15,
                dietType = "omnivore",
                mealType = "lunch",
                ingredients = """
150g chicken breast
3 cups mixed greens (lettuce, spinach, arugula)
1/2 cup cherry tomatoes
1/4 cucumber (sliced)
1/4 red onion (sliced)
2 tbsp balsamic vinaigrette
Salt, pepper, garlic powder
                """.trimIndent(),
                instructions = """
1. Season chicken with salt, pepper, garlic powder
2. Grill chicken for 6-7 minutes per side until cooked (internal temp 165°F)
3. Let chicken rest for 5 minutes
4. Prepare salad base with mixed greens
5. Add tomatoes, cucumber, and onion
6. Slice grilled chicken
7. Place chicken on salad
8. Drizzle with balsamic vinaigrette
9. Toss and serve
                """.trimIndent(),
                tags = "high-protein,low-carb,gluten-free"
            ))
            
            recipeDao.insert(Recipe(
                name = "Quinoa Buddha Bowl",
                description = "Nutritious bowl with quinoa, chickpeas, and roasted vegetables",
                caloriesPerServing = 420,
                proteinPerServing = 18,
                carbsPerServing = 62,
                fatPerServing = 12,
                servings = 1,
                prepTimeMinutes = 15,
                cookTimeMinutes = 25,
                dietType = "vegan",
                mealType = "lunch",
                ingredients = """
1/2 cup quinoa (uncooked)
1/2 cup chickpeas (cooked)
1 cup mixed vegetables (broccoli, carrots, bell peppers)
1/4 avocado (sliced)
2 tbsp tahini dressing
1 tsp olive oil
Salt, pepper, cumin
                """.trimIndent(),
                instructions = """
1. Cook quinoa according to package (usually 15 minutes)
2. Preheat oven to 400°F
3. Toss vegetables with olive oil, salt, pepper, cumin
4. Roast vegetables for 20 minutes
5. Heat chickpeas in pan
6. Assemble bowl: quinoa base, roasted veggies, chickpeas
7. Top with avocado slices
8. Drizzle tahini dressing
9. Serve warm
                """.trimIndent(),
                tags = "vegan,high-fiber,gluten-free"
            ))
            
            recipeDao.insert(Recipe(
                name = "Tuna Avocado Wrap",
                description = "Quick whole wheat wrap with tuna salad and fresh veggies",
                caloriesPerServing = 380,
                proteinPerServing = 32,
                carbsPerServing = 36,
                fatPerServing = 14,
                servings = 1,
                prepTimeMinutes = 10,
                cookTimeMinutes = 0,
                dietType = "omnivore",
                mealType = "lunch",
                ingredients = """
1 can tuna (in water, drained)
1/4 avocado (mashed)
1 tbsp Greek yogurt
1 whole wheat tortilla
1/2 cup mixed greens
1/4 cup tomatoes (diced)
1/4 cup cucumber (diced)
Salt, pepper, lemon juice
                """.trimIndent(),
                instructions = """
1. Mix tuna, mashed avocado, Greek yogurt in bowl
2. Add salt, pepper, lemon juice to taste
3. Warm tortilla slightly
4. Spread tuna mixture on tortilla
5. Add mixed greens, tomatoes, cucumber
6. Roll tightly
7. Cut in half diagonally
8. Serve immediately or wrap in foil
                """.trimIndent(),
                tags = "high-protein,quick,no-cook"
            ))
            
            // Dinner Recipes
            recipeDao.insert(Recipe(
                name = "Grilled Salmon with Broccoli",
                description = "Omega-3 rich salmon with garlic roasted broccoli",
                caloriesPerServing = 420,
                proteinPerServing = 38,
                carbsPerServing = 12,
                fatPerServing = 24,
                servings = 1,
                prepTimeMinutes = 10,
                cookTimeMinutes = 20,
                dietType = "omnivore",
                mealType = "dinner",
                ingredients = """
150g salmon fillet
2 cups broccoli florets
2 cloves garlic (minced)
1 tbsp olive oil
Lemon wedges
Salt, pepper, paprika
                """.trimIndent(),
                instructions = """
1. Preheat oven to 400°F
2. Season salmon with salt, pepper, paprika
3. Toss broccoli with olive oil, garlic, salt
4. Place salmon on baking sheet
5. Arrange broccoli around salmon
6. Bake for 15-18 minutes (salmon flakes easily)
7. Squeeze fresh lemon juice over salmon
8. Serve hot
                """.trimIndent(),
                tags = "high-protein,low-carb,omega-3,gluten-free"
            ))
            
            recipeDao.insert(Recipe(
                name = "Chicken Stir-Fry with Brown Rice",
                description = "Colorful vegetable stir-fry with chicken and brown rice",
                caloriesPerServing = 480,
                proteinPerServing = 42,
                carbsPerServing = 52,
                fatPerServing = 12,
                servings = 1,
                prepTimeMinutes = 15,
                cookTimeMinutes = 20,
                dietType = "omnivore",
                mealType = "dinner",
                ingredients = """
150g chicken breast (sliced)
1/2 cup brown rice (uncooked)
1 cup mixed vegetables (bell peppers, snap peas, carrots)
2 cloves garlic (minced)
1 tbsp low-sodium soy sauce
1 tsp sesame oil
1 tsp ginger (grated)
                """.trimIndent(),
                instructions = """
1. Cook brown rice according to package (40 minutes)
2. Slice chicken into strips
3. Heat sesame oil in wok or large pan
4. Stir-fry chicken for 5-6 minutes until cooked
5. Remove chicken, set aside
6. Stir-fry vegetables with garlic and ginger (3-4 min)
7. Return chicken to pan
8. Add soy sauce and toss
9. Serve over brown rice
                """.trimIndent(),
                tags = "high-protein,balanced"
            ))
            
            recipeDao.insert(Recipe(
                name = "Lentil Curry with Spinach",
                description = "Hearty plant-based curry packed with protein and iron",
                caloriesPerServing = 380,
                proteinPerServing = 20,
                carbsPerServing = 58,
                fatPerServing = 8,
                servings = 2,
                prepTimeMinutes = 10,
                cookTimeMinutes = 30,
                dietType = "vegan",
                mealType = "dinner",
                ingredients = """
1 cup red lentils (dried)
2 cups spinach (fresh)
1 can diced tomatoes
1 onion (diced)
2 cloves garlic (minced)
1 tbsp curry powder
1 cup coconut milk (light)
2 cups vegetable broth
Salt to taste
                """.trimIndent(),
                instructions = """
1. Rinse lentils thoroughly
2. Sauté onion and garlic in pot (3 min)
3. Add curry powder, cook 1 minute
4. Add lentils, tomatoes, broth
5. Bring to boil, then simmer 20 minutes
6. Stir in coconut milk and spinach
7. Cook 5 more minutes until spinach wilts
8. Season with salt
9. Serve over rice or with naan
                """.trimIndent(),
                tags = "vegan,high-protein,high-fiber"
            ))
            
            // Snack Recipes
            recipeDao.insert(Recipe(
                name = "Protein Energy Balls",
                description = "No-bake protein-packed snack balls",
                caloriesPerServing = 180,
                proteinPerServing = 8,
                carbsPerServing = 22,
                fatPerServing = 7,
                servings = 6,
                prepTimeMinutes = 15,
                cookTimeMinutes = 0,
                dietType = "vegetarian",
                mealType = "snack",
                ingredients = """
1 cup rolled oats
1/2 cup peanut butter
1/4 cup honey
1/4 cup chocolate chips
2 tbsp chia seeds
1 tsp vanilla extract
                """.trimIndent(),
                instructions = """
1. Mix all ingredients in bowl
2. Stir until well combined
3. Refrigerate mixture for 30 minutes
4. Roll into 12 balls (about 1 inch each)
5. Store in airtight container in fridge
6. Enjoy 1-2 balls as a snack
7. Keeps for 1 week refrigerated
                """.trimIndent(),
                tags = "vegetarian,high-protein,make-ahead,no-cook"
            ))
            
            recipeDao.insert(Recipe(
                name = "Hummus with Veggie Sticks",
                description = "Creamy chickpea hummus with fresh crunchy vegetables",
                caloriesPerServing = 220,
                proteinPerServing = 9,
                carbsPerServing = 28,
                fatPerServing = 8,
                servings = 1,
                prepTimeMinutes = 10,
                cookTimeMinutes = 0,
                dietType = "vegan",
                mealType = "snack",
                ingredients = """
1/2 cup hummus
1 cup raw vegetables (carrots, celery, bell peppers, cucumber)
                """.trimIndent(),
                instructions = """
1. Wash and cut vegetables into sticks
2. Arrange vegetables on plate
3. Place hummus in small bowl for dipping
4. Enjoy immediately
                """.trimIndent(),
                tags = "vegan,no-cook,quick,high-fiber"
            ))
            
            android.util.Log.d("SampleRecipes", "Successfully inserted sample recipes")
        }
    }
}
