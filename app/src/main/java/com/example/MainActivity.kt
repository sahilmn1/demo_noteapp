package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.ArchiveTrashScreen
import com.example.ui.screens.NoteEditScreen
import com.example.ui.screens.NotesHomeScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.theme.MemoFlowTheme
import com.example.ui.viewmodel.NotesViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    private val viewModel: NotesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val openNoteId = intent.getLongExtra("open_note_id", -1L)

        setContent {
            MemoFlowTheme {
                // Permission Request on Android 13+
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    // Notification permission state handled
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    MemoFlowNavGraph(
                        viewModel = viewModel,
                        initialOpenNoteId = if (openNoteId != -1L) openNoteId else null
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun MemoFlowNavGraph(
    viewModel: NotesViewModel,
    initialOpenNoteId: Long?
) {
    val navController = rememberNavController()

    LaunchedEffect(initialOpenNoteId) {
        if (initialOpenNoteId != null && initialOpenNoteId > 0) {
            navController.navigate("edit?noteId=$initialOpenNoteId&isChecklist=false&isReminder=false&speech=")
        }
    }

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            NotesHomeScreen(
                viewModel = viewModel,
                onNavigateToEdit = { noteId, isChecklist, isReminder, speech ->
                    val encodedSpeech = if (!speech.isNullOrBlank()) {
                        URLEncoder.encode(speech, StandardCharsets.UTF_8.toString())
                    } else ""
                    val targetId = noteId ?: 0L
                    navController.navigate("edit?noteId=$targetId&isChecklist=$isChecklist&isReminder=$isReminder&speech=$encodedSpeech")
                },
                onNavigateToReminders = {
                    navController.navigate("reminders")
                },
                onNavigateToArchiveTrash = {
                    navController.navigate("archive_trash")
                }
            )
        }

        composable(
            route = "edit?noteId={noteId}&isChecklist={isChecklist}&isReminder={isReminder}&speech={speech}",
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.LongType
                    defaultValue = 0L
                },
                navArgument("isChecklist") {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument("isReminder") {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument("speech") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId")?.takeIf { it > 0 }
            val isChecklist = backStackEntry.arguments?.getBoolean("isChecklist") ?: false
            val isReminder = backStackEntry.arguments?.getBoolean("isReminder") ?: false
            val rawSpeech = backStackEntry.arguments?.getString("speech") ?: ""
            val decodedSpeech = if (rawSpeech.isNotBlank()) {
                try {
                    URLDecoder.decode(rawSpeech, StandardCharsets.UTF_8.toString())
                } catch (e: Exception) {
                    rawSpeech
                }
            } else null

            NoteEditScreen(
                noteId = noteId,
                isChecklistInitial = isChecklist,
                openReminderInitial = isReminder,
                initialSpeechText = decodedSpeech,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("reminders") {
            RemindersScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEdit = { noteId ->
                    navController.navigate("edit?noteId=$noteId&isChecklist=false&isReminder=false&speech=")
                },
                onAddNewReminder = {
                    navController.navigate("edit?noteId=0&isChecklist=false&isReminder=true&speech=")
                }
            )
        }

        composable("archive_trash") {
            ArchiveTrashScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
