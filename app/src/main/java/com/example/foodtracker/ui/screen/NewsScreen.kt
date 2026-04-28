package com.example.foodtracker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.example.foodtracker.data.model.NewsResponse
import com.example.foodtracker.data.model.Article
import com.example.foodtracker.data.repository.NewsRepository
import com.example.foodtracker.ui.theme.AppBackground
import com.example.foodtracker.ui.theme.AppSurface
import com.example.foodtracker.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

    private suspend fun loadNews(
    scope: CoroutineScope,
    newsRepository: NewsRepository,
    onComplete: (List<NewsResponse.Article>, String?) -> Unit
) {
    try {
        val result = newsRepository.getLatestNews()
        result.onSuccess { response ->
            onComplete(response.articles, null)
        }.onFailure { exception ->
            onComplete(emptyList(), "Error loading news: ${exception.localizedMessage}")
        }
    } catch (e: Exception) {
        onComplete(emptyList(), "Unexpected error: ${e.localizedMessage}")
    }
}

@Composable
fun NewsScreen(padding: PaddingValues = PaddingValues(0.dp)) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val newsRepository = remember { NewsRepository() }
    var articles by remember { mutableStateOf<List<NewsResponse.Article>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current

    // Fetch news when the screen is first displayed
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    error = "No internet connection. Please check your network settings and try again."
                    return@launch
                }
                val result = newsRepository.getLatestNews()
                result.onSuccess { response ->
                    articles = response.articles
                    error = null
                }.onFailure { exception ->
                    error = exception.message
                }
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(padding)
    ) {
        Text(
            "Health & Nutrition News",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Unable to load news",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No internet connection. Please check your network settings and try again.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                isLoading = true
                                error = null
                                scope.launch {
                                    try {
                                        if (!NetworkUtils.isNetworkAvailable(context)) {
                                            error = "No internet connection. Please check your network settings and try again."
                                            return@launch
                                        }
                                        val result = newsRepository.getLatestNews()
                                        result.onSuccess { response ->
                                            articles = response.articles
                                            error = null
                                        }.onFailure { exception ->
                                            error = exception.message
                                        }
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(articles) { article ->
                        NewsCard(article = article, onArticleClick = {
                            article.url.let { uriHandler.openUri(it) }
                        })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewsCard(
    article: NewsResponse.Article,
    onArticleClick: () -> Unit
) {
    Card(
        onClick = onArticleClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            article.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3
                )
                Spacer(Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = article.source.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = article.publishedAt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
