package pe.edu.upeu.solshiran.presentation.auth

import androidx.compose.material3.*
import androidx.compose.runtime.*
import pe.edu.upeu.solshiran.presentation.*
import pe.edu.upeu.solshiran.presentation.components.*

@Composable fun LoginScreen(vm: AppViewModel,registro: Boolean) {
    var nombre by remember {mutableStateOf("")}
    var correo by remember {mutableStateOf("")}
    var password by remember {mutableStateOf("")}
    val state by vm.state.collectAsState()
    Text(if(registro) "Crea tu cuenta" else "Bienvenido de nuevo",style=MaterialTheme.typography.headlineMedium)
    Text("Encuentra tu espacio para descansar.")
    Tarjeta {
        if(registro) Campo("Nombre completo",nombre,{nombre=it})
        Campo("Correo electrónico",correo,{correo=it})
        Campo("Contraseña",password,{password=it},secreto=true)
        Accion(if(registro) "Crear cuenta" else "Iniciar sesión",!state.ocupado && correo.isNotBlank() && password.isNotBlank()) {
            if(registro) vm.registrar(nombre,correo,password) else vm.entrar(correo,password)
        }
        TextButton({vm.ir(if(registro) "login" else "registro")},enabled=!state.ocupado) {
            Text(if(registro) "Ya tengo una cuenta" else "Crear una cuenta de cliente")
        }
    }
}
