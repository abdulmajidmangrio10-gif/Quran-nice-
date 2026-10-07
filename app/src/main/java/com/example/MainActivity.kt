package com.example
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.QuranVideoEditorTheme
import com.example.viewmodel.EditorViewModel

enum class AppScreen {
    HOME,
    EDITOR
}

class MainActivity : ComponentActivity() {

    private val editorViewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuranVideoEditorTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.HOME) }

                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen_transition",
                    modifier = Modifier.fillMaxSize()
                ) { screen ->
                    when (screen) {
                        AppScreen.HOME -> {
                            HomeScreen(
                                onOpenEditorWithVideo = { uri, durationMs, name ->
                                    editorViewModel.setInitialVideo(uri, durationMs, name)
                                    currentScreen = AppScreen.EDITOR
                                },
                                onOpenEditorBlank = {
                                    editorViewModel.createBlankProject()
                                    currentScreen = AppScreen.EDITOR
                                }
                            )
                        }

                        AppScreen.EDITOR -> {
                            EditorScreen(
                                viewModel = editorViewModel,
                                onNavigateBack = {
                                    currentScreen = AppScreen.HOME
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
