package com.cem.appllamadas.presentation.contacto

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cem.appllamadas.call.CallState
import com.cem.appllamadas.domain.model.Contacto
import com.cem.appllamadas.domain.model.EstadoContacto
import com.cem.appllamadas.domain.model.ResultadoLlamada

// ─── Estructura de Tipificaciones Jerárquicas ────────────────────────────────
private val TIPIFICACIONES_POR_RESULTADO = mapOf(
    ResultadoLlamada.CONTACTADO_EFECTIVO to listOf(
        "ENCUESTA_COMPLETA", "ENCUESTA_PARCIAL", "AGENDA_CALLBACK", "DERIVADO_A_OTRO"
    ),
    ResultadoLlamada.CONTACTADO_NO_EFECTIVO to listOf(
        "RECHAZO_EXPLICITO", "SIN_TIEMPO", "CORTA_LLAMADA", "IDIOMA_DISTINTO", 
        "FUERA_DE_SEGMENTO", "PERSONA_EQUIVOCADA", "NO_CONTACTAR_NUEVAMENTE"
    ),
    ResultadoLlamada.NO_CONTACTADO to listOf(
        "NO_CONTESTA", "TELEFONO_APAGADO", "OCUPADO", "BUZON_DE_VOZ", 
        "NUMERO_INVALIDO", "LLAMADA_CAIDA", "FAX_O_TONO"
    )
)

private val MOTIVOS_DISPONIBLES = listOf(
    "N/A (General)", "GESTION_EXITOSA", "TRABAJANDO", "OCUPADO",
    "EN_TRANSITO", "MALA_SENAL", "DESCONFIANZA", "NO_INTERES", 
    "MALA_EXPERIENCIA", "NO_QUIERE_DATOS", "IDIOMA_DISTINTO",
    "POCO_TIEMPO", "SOLICITA_LLAMADA_LUEGO", "DATOS_ERRONEOS"
)

// ─── Anti-fraude: duración mínima para permitir ciertos estados ──────────────
private const val MIN_CALL_DURATION_SEC = 20

// ─── Colores por estado ───────────────────────────────────────────────────────
private fun estadoColor(estado: EstadoContacto) = when (estado) {
    EstadoContacto.PENDIENTE   -> Color(0xFF6366F1)
    EstadoContacto.EN_GESTION  -> Color(0xFFF59E0B)
    EstadoContacto.CONTACTADO  -> Color(0xFF10B981)
    EstadoContacto.DESISTIDO   -> Color(0xFFEF4444)
}

private fun estadoLabel(estado: EstadoContacto) = when (estado) {
    EstadoContacto.PENDIENTE   -> "Pendiente"
    EstadoContacto.EN_GESTION  -> "En gestión"
    EstadoContacto.CONTACTADO  -> "Contactado"
    EstadoContacto.DESISTIDO   -> "Desistido"
}

// ═══════════════════════════════════════════════════════════════════════════════
// PANTALLA PRINCIPAL — enruta entre listado, detalle y post-llamada
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactoScreen(
    viewModel: ContactoViewModel,
    onLogout: () -> Unit,
    onAbrirEncuesta: (String) -> Unit
) {
    val mostrarListado by viewModel.mostrarListado.collectAsState()
    val contacto       by viewModel.contactoActual.collectAsState()
    val callState      by viewModel.callStateManager.callState.collectAsState()
    val postCall       by viewModel.postCallState.collectAsState()
    val mostrarDialog  by viewModel.mostrarEncuestaDialog.collectAsState()
    val isLoading      by viewModel.isLoading.collectAsState()
    val errorConcurrencia by viewModel.errorConcurrencia.collectAsState()

    // Manejo de Error de Concurrencia (Pool Model)
    if (errorConcurrencia != null) {
        AlertDialog(
            onDismissRequest = { viewModel.resetErrorConcurrencia() },
            title = { Text("Contacto No Disponible") },
            text = { Text(errorConcurrencia!!) },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.resetErrorConcurrencia()
                    viewModel.volverAlListado() 
                }) {
                    Text("Entendido")
                }
            }
        )
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
            Card(shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(16.dp))
                    Text("Verificando disponibilidad...")
                }
            }
        }
    }

    when {
        // 1. Formulario post-llamada
        postCall != null -> PostCallForm(
            duracion  = postCall!!.duracion,
            onConfirmar = { resultado, tipo, motivo, obs ->
                viewModel.confirmarRegistro(resultado, tipo, motivo, obs)
            },
            onCancelar = { viewModel.volverAlListado() }
        )

        // 2. Llamada en curso
        callState is CallState.Calling || callState is CallState.Answered ->
            CallInProgressScreen(answered = callState is CallState.Answered)

        // 3. Detalle de contacto
        !mostrarListado && contacto != null -> ContactoDetalleScreen(
            contacto = contacto!!,
            viewModel = viewModel
        )

        // 4. Listado de contactos
        else -> ContactoListadoScreen(viewModel = viewModel, onLogout = onLogout)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// LISTADO DE CONTACTOS
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactoListadoScreen(viewModel: ContactoViewModel, onLogout: () -> Unit) {
    val contactos by viewModel.todosLosContactos.collectAsState()
    val pendientes = contactos.filter {
        it.estado != EstadoContacto.DESISTIDO && it.estado != EstadoContacto.CONTACTADO
    }
    val gestionados = contactos.filter {
        it.estado == EstadoContacto.CONTACTADO || it.estado == EstadoContacto.DESISTIDO
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("App Llamadas", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${pendientes.size} pendientes · ${contactos.size} total",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.forceRefresh() }) {
                        Icon(androidx.compose.material.icons.filled.Refresh, contentDescription = "Actualizar", tint = Color.White)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar sesión")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6366F1),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (contactos.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No hay contactos cargados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (pendientes.isNotEmpty()) {
                item {
                    Text("PENDIENTES", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp))
                }
                items(pendientes, key = { it.id }) { c ->
                    ContactoListItem(contacto = c, onClick = { viewModel.seleccionarContacto(c) })
                }
            }
            if (gestionados.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("GESTIONADOS", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp))
                }
                items(gestionados, key = { it.id }) { c ->
                    ContactoListItem(contacto = c, onClick = { viewModel.seleccionarContacto(c) })
                }
            }
        }
    }
}

@Composable
fun ContactoListItem(contacto: Contacto, onClick: () -> Unit) {
    val color = estadoColor(contacto.estado)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape    = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(contacto.nombre, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(contacto.telefono, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.15f)) {
                        Text(estadoLabel(contacto.estado), color = color, fontSize = 11.sp,
                            fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("${contacto.intentos}/5", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            // Info de última gestión
            if (contacto.ultimaTipificacion != null) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.alpha(0.1f))
                Spacer(Modifier.height(8.dp))
                Column {
                    Text(
                        text = "Última gestión: ${contacto.ultimaTipificacion}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!contacto.ultimaObservacion.isNullOrBlank()) {
                        Text(
                            text = contacto.ultimaObservacion,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// DETALLE DE CONTACTO
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactoDetalleScreen(contacto: Contacto, viewModel: ContactoViewModel) {
    val context = LocalContext.current

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val canCall = permissions[Manifest.permission.CALL_PHONE] ?: false
        val canReadState = permissions[Manifest.permission.READ_PHONE_STATE] ?: false

        if (canCall && canReadState) {
            viewModel.intentarBloquearContacto(contacto.id) {
                viewModel.iniciarLlamada()
                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${contacto.telefono}"))
                context.startActivity(intent)
            }
        }
    }

    val bloqueado = contacto.intentos >= 5 || contacto.estado == EstadoContacto.DESISTIDO

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contacto.nombre, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.volverAlListado() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6366F1),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card info del contacto
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF6366F1).copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF6366F1))
                        Spacer(Modifier.width(8.dp))
                        Text(contacto.nombre, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Text("📞 ${contacto.telefono}", fontSize = 15.sp)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val color = estadoColor(contacto.estado)
                        Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f)) {
                            Text(estadoLabel(contacto.estado), color = color, fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                        Surface(shape = RoundedCornerShape(8.dp),
                            color = (if (bloqueado) Color(0xFFEF4444) else Color(0xFF10B981)).copy(alpha = 0.15f)) {
                            Text("${contacto.intentos}/5 intentos",
                                color = if (bloqueado) Color(0xFFEF4444) else Color(0xFF10B981),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }

                    if (contacto.ultimaTipificacion != null) {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(modifier = Modifier.alpha(0.1f))
                        Spacer(Modifier.height(8.dp))
                        Text("Última gestión:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text(contacto.ultimaTipificacion!!, fontSize = 14.sp)
                        if (!contacto.ultimaObservacion.isNullOrBlank()) {
                            Text(contacto.ultimaObservacion!!, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Alerta si está bloqueado
            AnimatedVisibility(visible = bloqueado) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🚫 Este contacto ha alcanzado el límite de 5 intentos. No se pueden realizar más llamadas.",
                        color = Color(0xFFEF4444), modifier = Modifier.padding(16.dp), fontSize = 14.sp)
                }
            }

            if (!bloqueado) {
                // Botón principal de llamada
                Button(
                    onClick = {
                        callPermissionLauncher.launch(
                            arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE)
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Icon(Icons.Default.Call, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Llamar ahora", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// LLAMADA EN CURSO
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun CallInProgressScreen(answered: Boolean) {
    Box(Modifier.fillMaxSize().background(Color(0xFF0F172A)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            CircularProgressIndicator(
                color = if (answered) Color(0xFF10B981) else Color(0xFF6366F1),
                strokeWidth = 4.dp,
                modifier = Modifier.size(56.dp)
            )
            Text(
                if (answered) "📞 Llamada en curso..." else "📲 Conectando...",
                color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold
            )
            Text(
                "Cuando cuelgues, se te pedirá que\nregistres el resultado de la llamada.",
                color = Color(0xFF94A3B8), fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// FORMULARIO POST-LLAMADA (resultado jerárquico)
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCallForm(
    duracion: Int,
    onConfirmar: (ResultadoLlamada, String, String?, String) -> Unit,
    onCancelar: () -> Unit
) {
    var resultadoSeleccionado by remember { mutableStateOf<ResultadoLlamada?>(null) }
    var tipificacion by remember { mutableStateOf<String?>(null) }
    var motivo       by remember { mutableStateOf<String?>(null) }
    var observacion  by remember { mutableStateOf("") }
    
    var expandedTip  by remember { mutableStateOf(false) }
    var expandedMot  by remember { mutableStateOf(false) }

    val noContestaDisabled = duracion < MIN_CALL_DURATION_SEC

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar resultado", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancelar) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Cancelar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6366F1),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (duracion > 0) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("⏱ Duración de la llamada: ${duracion}s",
                        color = Color(0xFF10B981), fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(16.dp))
                }
            }

            // 1. RESULTADO PRINCIPAL
            Text("1. Resultado Principal *", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            
            // Warning if not answered enough
            if (noContestaDisabled) {
              Text("⚠ Ciertos estados de \"No Contactado\" requieren una mayor duración detectada.", 
                  color = Color(0xFFF59E0B), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp))
            }

            ResultadoSelector(
                selected = resultadoSeleccionado,
                onSelect = { 
                    resultadoSeleccionado = it
                    tipificacion = null 
                    motivo = null
                }
            )

            // 2. TIPIFICACIÓN (Dependiente)
            AnimatedVisibility(visible = resultadoSeleccionado != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("2. Tipificación *", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    val opciones = TIPIFICACIONES_POR_RESULTADO[resultadoSeleccionado] ?: emptyList()
                    ExposedDropdownMenuBox(expanded = expandedTip, onExpandedChange = { expandedTip = it }) {
                        OutlinedTextField(
                            value = tipificacion ?: "Seleccionar...",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedTip) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            isError = tipificacion == null
                        )
                        ExposedDropdownMenu(expanded = expandedTip, onDismissRequest = { expandedTip = false }) {
                            opciones.forEach { opcion ->
                                DropdownMenuItem(
                                    text = { Text(opcion) },
                                    onClick = { 
                                        tipificacion = opcion
                                        motivo = null
                                        expandedTip = false 
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 3. MOTIVO (Siempre visible y obligatorio)
            AnimatedVisibility(visible = tipificacion != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("3. Motivo *", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    ExposedDropdownMenuBox(expanded = expandedMot, onExpandedChange = { expandedMot = it }) {
                        OutlinedTextField(
                            value = motivo ?: "Seleccionar motivo...",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedMot) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            isError = tipificacion != null && motivo == null
                        )
                        ExposedDropdownMenu(expanded = expandedMot, onDismissRequest = { expandedMot = false }) {
                            MOTIVOS_DISPONIBLES.forEach { opcion ->
                                DropdownMenuItem(
                                    text = { Text(opcion) },
                                    onClick = { motivo = opcion; expandedMot = false }
                                )
                            }
                        }
                    }
                }
            }

            // 4. OBSERVACIÓN
            OutlinedTextField(
                value = observacion,
                onValueChange = { observacion = it },
                label = { Text("Observación adicional") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3
            )

            Spacer(Modifier.height(8.dp))

            // GUARDAR
            Button(
                onClick = {
                    if (resultadoSeleccionado != null && tipificacion != null && motivo != null) {
                        onConfirmar(resultadoSeleccionado!!, tipificacion!!, motivo, observacion)
                    }
                },
                enabled = resultadoSeleccionado != null && tipificacion != null && motivo != null,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Guardar Gestión", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun ResultadoSelector(
    selected: ResultadoLlamada?,
    onSelect: (ResultadoLlamada) -> Unit
) {
    val opciones = listOf(
        ResultadoLlamada.CONTACTADO_EFECTIVO    to ("Efectivo"     to Color(0xFF10B981)),
        ResultadoLlamada.CONTACTADO_NO_EFECTIVO to ("No Efectivo"  to Color(0xFFF59E0B)),
        ResultadoLlamada.NO_CONTACTADO         to ("No Contactado" to Color(0xFFEF4444))
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        opciones.forEach { (resultado, labelColor) ->
            val (label, color) = labelColor
            val isSelected = selected == resultado
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clickable { onSelect(resultado) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) color else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

