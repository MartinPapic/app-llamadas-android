package com.cem.appllamadas

// Trigger redeploy: Pool Model & Sync UI Integration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.activity.viewModels
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.cem.appllamadas.data.local.SessionManager
import com.cem.appllamadas.presentation.contacto.ContactoScreen
import com.cem.appllamadas.presentation.contacto.ContactoViewModel
import com.cem.appllamadas.presentation.login.LoginScreen
import com.cem.appllamadas.presentation.login.LoginViewModel
import com.cem.appllamadas.presentation.proyecto.ProjectSelectionScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    // ViewModel compartido entre todas las rutas — scoped a la Activity
    private val contactoViewModel: ContactoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Si hay sesión activa, ir a selección de proyecto; si no, al login
                    val startDestination = if (sessionManager.isLoggedIn()) "seleccion_proyecto" else "login"

                    NavHost(navController = navController, startDestination = startDestination) {

                        composable("login") {
                            val viewModel = hiltViewModel<LoginViewModel>()
                            LoginScreen(
                                viewModel = viewModel,
                                onLoginSuccess = {
                                    navController.navigate("seleccion_proyecto") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("seleccion_proyecto") {
                            ProjectSelectionScreen(
                                viewModel = contactoViewModel,
                                onProjectSelected = {
                                    navController.navigate("contacto")
                                },
                                onLogout = {
                                    contactoViewModel.logout()
                                    navController.navigate("login") {
                                        popUpTo(navController.graph.id) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("contacto") {
                            val proyectoSeleccionado by contactoViewModel.proyectoSeleccionado.collectAsState()

                            if (proyectoSeleccionado == null) {
                                navController.navigate("seleccion_proyecto") {
                                    popUpTo("contacto") { inclusive = true }
                                }
                            }

                            ContactoScreen(
                                viewModel = contactoViewModel,
                                onLogout = {
                                    contactoViewModel.logout()
                                    navController.navigate("login") {
                                        popUpTo(navController.graph.id) { inclusive = true }
                                    }
                                },
                                onVolverAProyectos = {
                                    contactoViewModel.deseleccionarProyecto()
                                    navController.navigate("seleccion_proyecto") {
                                        popUpTo("contacto") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
