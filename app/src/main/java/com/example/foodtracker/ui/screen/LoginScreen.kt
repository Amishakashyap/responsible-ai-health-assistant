package com.example.foodtracker.ui.screen

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodtracker.data.db.AppDatabase
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (Long) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToGuestSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val scope = rememberCoroutineScope()
    
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    // Google Sign-In configuration
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }
    
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                // Handle successful Google sign in
                scope.launch {
                    try {
                        val googleEmail = account?.email ?: ""
                        val googleName = account?.displayName ?: "User"
                        
                        // Check if user exists
                        var user = db.userDao().loginByEmail(googleEmail)
                        
                        if (user == null) {
                            // Auto-register with Google account
                            val newUserId = db.userDao().insert(
                                com.example.foodtracker.data.db.User(
                                    id = 0,
                                    name = googleName,
                                    email = googleEmail,
                                    city = "Not Provided",
                                    gender = "Not Specified",
                                    age = 0,
                                    bloodGroup = "Not Provided"
                                )
                            )
                            user = db.userDao().getById(newUserId)
                        }
                        
                        if (user != null) {
                            val userPrefs = com.example.foodtracker.data.user.UserPreferences(context)
                            userPrefs.userId = user.id
                            Toast.makeText(context, "Welcome, ${user.name}!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess(user.id)
                        }
                    } catch (e: Exception) {
                        errorMsg = "Google sign-in failed: ${e.message}"
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: ApiException) {
                errorMsg = "Google sign-in failed: ${e.message}"
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0D0D),
                        Color(0xFF15192D),
                        Color(0xFF003E35)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Fitness Icon
            Text(
                "🔥",
                fontSize = 56.sp
            )
            Spacer(Modifier.height(16.dp))
            // App Title with modern design
            Text(
                "SwasthVision",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Your Fitness Journey Starts Here",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )
        
        Spacer(Modifier.height(40.dp))
        
        Text(
            "Login",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(24.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Your Name") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        
        if (errorMsg.isNotEmpty()) {
            Text(errorMsg, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }
        
        Button(
            onClick = {
                if (email.isNotEmpty() && name.isNotEmpty()) {
                    isLoading = true
                    errorMsg = ""
                    scope.launch {
                        try {
                            val user = db.userDao().loginByEmail(email.trim())
                            if (user != null && user.name.equals(name.trim(), ignoreCase = true)) {
                                val userPrefs = com.example.foodtracker.data.user.UserPreferences(context)
                                userPrefs.userId = user.id
                                onLoginSuccess(user.id)
                            } else {
                                errorMsg = "Name or email doesn't match any account"
                            }
                        } catch (e: Exception) {
                            errorMsg = "Login failed: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                } else {
                    errorMsg = "Please enter your name and email"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Login")
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Don't have an account?",
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = onNavigateToRegister, enabled = !isLoading) {
                Text("Register", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                "  OR  ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Social Login Buttons with modern design and proper icons
        Button(
            onClick = {
                isLoading = true
                errorMsg = ""
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                isLoading = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                GoogleIcon(modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    "Continue with Google",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        Button(
            onClick = { 
                isLoading = true
                errorMsg = ""
                // Facebook login implementation would go here
                Toast.makeText(context, "Facebook login - Use email login for now", Toast.LENGTH_LONG).show()
                isLoading = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1877F2),
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                FacebookIcon(modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    "Continue with Facebook",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        HorizontalDivider()
        
        Spacer(Modifier.height(24.dp))
        
        // Guest Login
        OutlinedButton(
            onClick = onNavigateToGuestSetup,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("👤 Continue as Guest", fontWeight = FontWeight.Medium)
        }
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            "You can create an account later from the Profile page",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(24.dp))
        
        // User Guide
        TextButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://youtu.be/8jNcXhwgdH8?si=bQ21z6rCMqutCuEV")
                }
                context.startActivity(intent)
            },
            enabled = !isLoading
        ) {
            Text(
                "📖 View User Guide",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Spacer(Modifier.height(32.dp))
        }
    }
}

// Custom Google Icon Vector
@Composable
fun GoogleIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = rememberGoogleIcon(),
        contentDescription = "Google",
        modifier = modifier,
        tint = Color.Unspecified
    )
}

@Composable
fun rememberGoogleIcon(): ImageVector {
    return remember {
        ImageVector.Builder(
            name = "google",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF4285F4)),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(22.56f, 12.25f)
                curveToRelative(0f, -0.78f, -0.07f, -1.53f, -0.2f, -2.25f)
                horizontalLineTo(12f)
                verticalLineToRelative(4.26f)
                horizontalLineToRelative(5.92f)
                curveToRelative(-0.26f, 1.37f, -1.04f, 2.53f, -2.21f, 3.31f)
                verticalLineToRelative(2.77f)
                horizontalLineToRelative(3.57f)
                curveToRelative(2.08f, -1.92f, 3.28f, -4.74f, 3.28f, -8.09f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF34A853)),
                fillAlpha = 1.0f
            ) {
                moveTo(12f, 23f)
                curveToRelative(2.97f, 0f, 5.46f, -0.98f, 7.28f, -2.66f)
                lineToRelative(-3.57f, -2.77f)
                curveToRelative(-0.98f, 0.66f, -2.23f, 1.06f, -3.71f, 1.06f)
                curveToRelative(-2.86f, 0f, -5.29f, -1.93f, -6.16f, -4.53f)
                horizontalLineTo(2.18f)
                verticalLineToRelative(2.84f)
                curveTo(3.99f, 20.53f, 7.7f, 23f, 12f, 23f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFFFBBC05)),
                fillAlpha = 1.0f
            ) {
                moveTo(5.84f, 14.09f)
                curveToRelative(-0.22f, -0.66f, -0.35f, -1.36f, -0.35f, -2.09f)
                reflectiveCurveToRelative(0.13f, -1.43f, 0.35f, -2.09f)
                verticalLineTo(7.07f)
                horizontalLineTo(2.18f)
                curveTo(1.43f, 8.55f, 1f, 10.22f, 1f, 12f)
                reflectiveCurveToRelative(0.43f, 3.45f, 1.18f, 4.93f)
                lineToRelative(2.85f, -2.22f)
                lineToRelative(0.81f, -0.62f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFFEA4335)),
                fillAlpha = 1.0f
            ) {
                moveTo(12f, 5.38f)
                curveToRelative(1.62f, 0f, 3.06f, 0.56f, 4.21f, 1.64f)
                lineToRelative(3.15f, -3.15f)
                curveTo(17.45f, 2.09f, 14.97f, 1f, 12f, 1f)
                curveTo(7.7f, 1f, 3.99f, 3.47f, 2.18f, 7.07f)
                lineToRelative(3.66f, 2.84f)
                curveToRelative(0.87f, -2.6f, 3.3f, -4.53f, 6.16f, -4.53f)
                close()
            }
        }.build()
    }
}

// Custom Facebook Icon
@Composable
fun FacebookIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = rememberFacebookIcon(),
        contentDescription = "Facebook",
        modifier = modifier,
        tint = Color(0xFF1877F2)
    )
}

@Composable
fun rememberFacebookIcon(): ImageVector {
    return remember {
        ImageVector.Builder(
            name = "facebook",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF1877F2)),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(24f, 12.073f)
                curveToRelative(0f, -6.627f, -5.373f, -12f, -12f, -12f)
                reflectiveCurveToRelative(-12f, 5.373f, -12f, 12f)
                curveToRelative(0f, 5.99f, 4.388f, 10.954f, 10.125f, 11.854f)
                verticalLineToRelative(-8.385f)
                horizontalLineTo(7.078f)
                verticalLineToRelative(-3.47f)
                horizontalLineToRelative(3.047f)
                verticalLineTo(9.43f)
                curveToRelative(0f, -3.007f, 1.792f, -4.669f, 4.533f, -4.669f)
                curveToRelative(1.312f, 0f, 2.686f, 0.235f, 2.686f, 0.235f)
                verticalLineToRelative(2.953f)
                horizontalLineTo(15.83f)
                curveToRelative(-1.491f, 0f, -1.956f, 0.925f, -1.956f, 1.874f)
                verticalLineToRelative(2.25f)
                horizontalLineToRelative(3.328f)
                lineToRelative(-0.532f, 3.47f)
                horizontalLineToRelative(-2.796f)
                verticalLineToRelative(8.385f)
                curveTo(19.612f, 23.027f, 24f, 18.062f, 24f, 12.073f)
                close()
            }
        }.build()
    }
}
