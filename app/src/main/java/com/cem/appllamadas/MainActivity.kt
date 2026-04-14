package com.cem.appllamadas

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

                    // Si hay sesión activa, ir directo a contacto; si no, al login
                    val startDestination = if (sessionManager.isLoggedIn()) "contacto" else "login"

                    NavHost(navController = navController, startDestination = startDestination) {

                        composable("login") {
                            val viewModel = hiltViewModel<LoginViewModel>()
                            LoginScreen(
                                viewModel = viewModel,
                                onLoginSuccess = {
                                    navController.navigate("contacto") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("contacto") {
                            val viewModel = hiltViewModel<ContactoViewModel>()
                            ContactoScreen(
                                viewModel = viewModel,
                                onLogout = {
                                    viewModel.logout()
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onAbrirEncuesta = { contactoId ->
                                    navController.navigate("encuesta/$contactoId")
                                }
                            )
                        }

                        composable("encuesta/{contactoId}") { backStackEntry ->
                            val contactoId = backStackEntry.arguments?.getString("contactoId") ?: ""
                            // Usamos URL de prueba según lo solicitado por el usuario
                            val url = "https://www.questionpro.com/t/demo?contacto_id=${contactoId}"
                            val viewModel = hiltViewModel<EncuestaViewModel>()

                            EncuestaScreen(
                                contactoId = contactoId,
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
