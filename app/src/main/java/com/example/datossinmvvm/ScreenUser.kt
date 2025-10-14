package com.example.datossinmvvm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.room.Room
import com.example.datossinmvvm.R
import com.example.datossinmvvm.User
import com.example.datossinmvvm.UserDao
import com.example.datossinmvvm.UserDatabase
import kotlinx.coroutines.launch

private const val CHANNEL_ID = "user_channel"
private const val NOTIFICATION_ID = 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenUser(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = remember { crearDatabase(context) }
    val dao = remember { db.userDao() }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var dataUser by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) Log.i("Permission", "Notification permission granted.")
        else Log.w("Permission", "Notification permission denied.")
    }

    LaunchedEffect(Unit) {
        createNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Usuarios") },
                actions = {
                    IconButton(onClick = {
                        if (firstName.isNotBlank() && lastName.isNotBlank()) {
                            val user = User(0, firstName, lastName)
                            coroutineScope.launch {
                                agregarUsuario(user = user, dao = dao)
                                dataUser = getUsers(dao = dao)
                            }
                            firstName = ""
                            lastName = ""
                        }
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Agregar Usuario")
                    }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            dataUser = getUsers(dao = dao)
                        }
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Listar Usuarios")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            TextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name: ") },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            TextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name:") },
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        val usersBeforeDelete = dao.getAll()
                        if (usersBeforeDelete.isNotEmpty()) {
                            eliminarUltimoUsuario(dao = dao)
                            dataUser = getUsers(dao = dao) // <-- CORRECCIÓN: Sin '.value'
                            sendDeletionNotification(context)
                        } else {
                            dataUser = "No hay usuarios para eliminar." // <-- CORRECCIÓN: Sin '.value'
                        }
                    }
                }
            ) {
                Text("Eliminar Último Usuario", fontSize = 16.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = dataUser, // <-- CORRECCIÓN: Sin '.value'
                fontSize = 20.sp
            )
        }
    }
}

// --- FUNCIONES AUXILIARES ---

// <-- CORRECCIÓN: 'crearDatabase' NO debe ser @Composable
fun crearDatabase(context: Context): UserDatabase {
    return Room.databaseBuilder(context, UserDatabase::class.java, "user_db").build()
}

// <-- CORRECCIÓN: Nombres de función en minúscula (camelCase)
suspend fun getUsers(dao: UserDao): String {
    val users = dao.getAll()
    return if (users.isEmpty()) {
        "No hay usuarios registrados."
    } else {
        users.joinToString(separator = "\n") { user ->
            "${user.uid}: ${user.firstName} - ${user.lastName}"
        }
    }
}

suspend fun agregarUsuario(user: User, dao: UserDao) {
    try {
        dao.insert(user)
    } catch (e: Exception) {
        Log.e("User", "Error: insert: ${e.message}")
    }
}

suspend fun eliminarUltimoUsuario(dao: UserDao) {
    try {
        dao.deleteLast()
    } catch (e: Exception) {
        Log.e("User", "Error: deleteLast: ${e.message}")
    }
}

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Notificaciones de Usuario"
        val descriptionText = "Canal para notificar acciones sobre usuarios"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

fun sendDeletionNotification(context: Context) {
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Usuario Eliminado")
        .setContentText("Se ha eliminado el último usuario de la lista.")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)

    with(NotificationManagerCompat.from(context)) {
        try {
            notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            Log.e("Notification", "Permission not granted to post notification.", e)
        }
    }
}