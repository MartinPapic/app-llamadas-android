package com.cem.appllamadas.presentation.encuesta

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cem.appllamadas.domain.model.EstadoEncuesta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncuestaScreen(
    contactoId: String,
    url: String,
    viewModel: EncuestaViewModel,
    onFinished: () -> Unit
) {
    var mostrarConfirmacion by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Encuesta QuestionPro", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { mostrarConfirmacion = true }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF6366F1))
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // In a real app, we would detect if the survey is completed via URL redirection
                        // For the MVP, the agent manually registers completion in the previous screen
                        // or we trigger `onFinished()` if QuestionPro redirects to a "success" URL.
                    }
                }
                loadUrl(url)
            }
        },
            update = { webView ->
                webView.loadUrl(url)
            }
        )

        if (mostrarConfirmacion) {
            AlertDialog(
                onDismissRequest = { mostrarConfirmacion = false },
                title = { Text("Finalizar Encuesta", fontWeight = FontWeight.Bold) },
                text = { Text("Selecciona el estado con el que terminó la encuesta:") },
                confirmButton = {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                mostrarConfirmacion = false
                                viewModel.registrarEstadoEncuesta(contactoId, url, EstadoEncuesta.COMPLETA, onFinished)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) { Text("Completa") }

                        Button(
                            onClick = {
                                mostrarConfirmacion = false
                                viewModel.registrarEstadoEncuesta(contactoId, url, EstadoEncuesta.INCOMPLETA, onFinished)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                        ) { Text("Incompleta") }

                        OutlinedButton(
                            onClick = {
                                mostrarConfirmacion = false
                                viewModel.registrarEstadoEncuesta(contactoId, url, EstadoEncuesta.NO_REALIZADA, onFinished)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("No realizada / Cerrar", color = Color(0xFFEF4444)) }
                    }
                }
            )
        }
    }
}

}
