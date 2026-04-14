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
                            ContactoScreen(viewModel)
                        }

                        composable("encuesta/{url}") { backStackEntry ->
                            val url = backStackEntry.arguments?.getString("url")
                                ?: "https://www.questionpro.com"
                            com.cem.appllamadas.presentation.encuesta.EncuestaScreen(url = url) {
                                navController.popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }
}
