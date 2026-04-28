package com.example.foodtracker.ui


import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.foodtracker.R
import android.util.Log



// Recipe model
data class Recipe(
    val name: String,
    val ingredients: String,
    val steps: String,
    val cuisine: String
)






class ChatBotActivity : AppCompatActivity() {

    lateinit var userInput: EditText
    lateinit var sendBtn: Button
    lateinit var chatBox: TextView

    private val recipeList = mutableListOf<Recipe>()   // ✅ only one list

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_bot)

        userInput = findViewById(R.id.userInput)
        sendBtn = findViewById(R.id.sendBtn)
        chatBox = findViewById(R.id.chatBox)

        loadRecipes()   // ✅ correct function

        sendBtn.setOnClickListener {
            val question = userInput.text.toString()
            val reply = getRecipeAnswer(question)

            chatBox.append("\n\nYou: $question")
            chatBox.append("\nBot: $reply")

            userInput.text.clear()
        }
    }

    // ✅ Correct CSV loader
    private fun loadRecipes() {
        val inputStream = assets.open("databases/recipe_dataset.csv")
        val reader = inputStream.bufferedReader()

        // Skip header
        reader.readLine()

        reader.forEachLine { line ->

            // Regex to split CSV properly (handles quoted commas)
            val parts = Regex(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")
                .split(line)
                .map { it.replace("\"", "").trim() }

            if (parts.size >= 4) {
                val name = parts[0]
                val ingredients = parts[1]
                val steps = parts[2]
                val cuisine = parts[3]

                recipeList.add(
                    Recipe(
                        name = name,
                        ingredients = ingredients,
                        steps = steps,
                        cuisine = cuisine
                    )
                )
            }
        }
    }



    private fun parseCSVLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var insideQuotes = false

        for (char in line) {
            when (char) {
                '"' -> insideQuotes = !insideQuotes
                ',' -> {
                    if (insideQuotes) {
                        current.append(char)
                    } else {
                        result.add(current.toString())
                        current = StringBuilder()
                    }
                }
                else -> current.append(char)
            }
        }

        result.add(current.toString())
        return result.map { it.trim().replace("\"", "") }
    }


    //Log.d("RECIPES", "Loaded recipes count: ${recipeList.size}")



    // ✅ Correct search logic
    private fun getRecipeAnswer(userQuestion: String): String {
        val query = userQuestion.lowercase().trim()

        for (recipe in recipeList) {
            if (recipe.name.lowercase().contains(query)) {

                return """
Dish: ${recipe.name}

Cuisine: ${recipe.cuisine}

Ingredients:
${recipe.ingredients}

Steps:
${recipe.steps}
""".trimIndent()
            }
        }

        return "Sorry, recipe not found for \"$userQuestion\"."
    }








}
