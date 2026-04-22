package com.cem.appllamadas

// Trigger redeploy: Pool Model & Sync UI Integration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.cem.appllamadas.data.local.SessionManager
import com.cem.appllamadas.presentation.contacto.ContactoScreen
import com.cem.appllamadas.presentation.contacto.ContactoViewModel
import com.cem.appllamadas.presentation.encuesta.EncuestaScreen
import com.cem.appllamadas.presentation.encuesta.EncuestaViewModel
import com.cem.appllamadas.presentation.login.LoginScreen
import com.cem.appllamadas.presentation.login.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

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
                            val viewModel = hiltViewModel<ContactoViewModel>()
                            com.cem.appllamadas.presentation.proyecto.ProjectSelectionScreen(
                                viewModel = viewModel,
                                onProjectSelected = {
                                    navController.navigate("contacto")
                                },
                                onLogout = {
                                    viewModel.logout()
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("contacto") {
                            val viewModel = hiltViewModel<ContactoViewModel>()
                            val proyectoSeleccionado by viewModel.proyectoSeleccionado.collectAsState()
                            
                            // Si por algún motivo perdemos el proyecto, volver a selección
                            if (proyectoSeleccionado == null) {
                                navController.navigate("seleccion_proyecto") {
                                    popUpTo("contacto") { inclusive = true }
                                }
                            }

                            ContactoScreen(
                                viewModel = viewModel,
                                onLogout = {
                                    viewModel.logout()
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onAbrirEncuesta = { url ->
                                    // Ahora pasamos la URL directamente desde el ViewModel
                                    navController.navigate("encuesta?url=${java.net.URLEncoder.encode(url, "UTF-8")}")
                                },
                                onVolverAProyectos = {
                                    viewModel.deseleccionarProyecto()
                                    navController.navigate("seleccion_proyecto") {
                                        popUpTo("contacto") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("encuesta?url={url}") { backStackEntry ->
                            val url = backStackEntry.arguments?.getString("url") ?: ""
                            val viewModel = hiltViewModel<EncuestaViewModel>()

                            EncuestaScreen(
                                contactoId = "temp", // El ID ya está en la URL si QuestionPro lo requiere
                                url = url,
                                viewModel = viewModel,
                                onFinished = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
