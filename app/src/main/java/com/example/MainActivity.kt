package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.HearthplanDatabase
import com.example.data.ProjectRepository
import com.example.ui.HearthplanViewModel
import com.example.ui.components.dialogs.BlankProjectDialog
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.TemplatesScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.HearthplanTheme

class MainActivity : ComponentActivity() {

    private val database by lazy { HearthplanDatabase.getDatabase(this) }
    private val repository by lazy { ProjectRepository(database.projectDao()) }

    private val viewModel: HearthplanViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HearthplanViewModel(repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HearthplanTheme {
                val currentProject by viewModel.currentProject.collectAsState()
                val userProjects by viewModel.userProjects.collectAsState()
                val builtinTemplates by viewModel.builtinTemplates.collectAsState()

                var showTemplatesScreen by remember { mutableStateOf(false) }
                var showBlankProjectDialog by remember { mutableStateOf(false) }

                Surface(modifier = Modifier.fillMaxSize()) {
                    when {
                        currentProject != null -> {
                            BackHandler {
                                viewModel.closeProject()
                            }
                            EditorScreen(
                                viewModel = viewModel,
                                onBackToHome = { viewModel.closeProject() }
                            )
                        }
                        showTemplatesScreen -> {
                            BackHandler {
                                showTemplatesScreen = false
                            }
                            TemplatesScreen(
                                templates = builtinTemplates,
                                onBack = { showTemplatesScreen = false },
                                onSelectTemplate = { template ->
                                    showTemplatesScreen = false
                                    viewModel.cloneTemplate(template)
                                }
                            )
                        }
                        else -> {
                            WelcomeScreen(
                                userProjects = userProjects,
                                onOpenBlankDialog = { showBlankProjectDialog = true },
                                onOpenTemplates = { showTemplatesScreen = true },
                                onOpenProject = { project -> viewModel.openProject(project) },
                                onDeleteProject = { id -> viewModel.deleteProject(id) },
                                onImportJson = { json -> viewModel.importProjectFromJson(json) },
                                onExportProjectPdf = { proj ->
                                    val file = viewModel.exportProjectToPdf(this@MainActivity, proj)
                                    if (file != null && file.exists()) {
                                        try {
                                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                                this@MainActivity,
                                                "${packageName}.fileprovider",
                                                file
                                            )
                                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                putExtra(android.content.Intent.EXTRA_SUBJECT, "${proj.name} - Hearthplan Drawing")
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            startActivity(android.content.Intent.createChooser(intent, "Share Floor Plan PDF"))
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(this@MainActivity, "Error sharing PDF: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        android.widget.Toast.makeText(this@MainActivity, "Failed to generate PDF.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }

                    if (showBlankProjectDialog) {
                        BlankProjectDialog(
                            onDismiss = { showBlankProjectDialog = false },
                            onCreateProject = { name, w1, w2, l1, l2, unit, floors ->
                                showBlankProjectDialog = false
                                viewModel.createBlankProject(name, w1, w2, l1, l2, unit, floors)
                            }
                        )
                    }
                }
            }
        }
    }
}
