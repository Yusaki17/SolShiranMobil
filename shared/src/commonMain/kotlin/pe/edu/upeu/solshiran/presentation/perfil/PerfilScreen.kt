package pe.edu.upeu.solshiran.presentation.perfil

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.edu.upeu.solshiran.domain.model.FotoElegida
import pe.edu.upeu.solshiran.platform.*
import pe.edu.upeu.solshiran.presentation.*
import pe.edu.upeu.solshiran.presentation.components.*

@Composable fun PerfilScreen(vm: AppViewModel,state: UiState,plataforma: Plataforma) {
    val perfil=state.perfil ?: return
    var nombre by remember(perfil) {mutableStateOf(perfil.nombre)}
    var correo by remember(perfil) {mutableStateOf(perfil.email)}
    var foto by remember {mutableStateOf<FotoElegida?>(null)}
    Text("Mi perfil",style=MaterialTheme.typography.headlineMedium)
    Tarjeta {
        val bytes=foto?.bytes ?: state.fotoPerfil
        if(bytes!=null) ImagenBytes(bytes,"Foto de perfil",Modifier.size(150.dp))
        else Text(perfil.nombre.take(1).uppercase(),style=MaterialTheme.typography.displayMedium)
        Text("JPG o PNG · Hasta 2 MB")
        OutlinedButton({plataforma.elegirFoto{foto=it}},enabled=!state.ocupado){Text("Seleccionar foto")}
        foto?.let { f -> Accion("Guardar foto",!state.ocupado){vm.fotoPerfil(f);foto=null} }
        if(state.fotoPerfil!=null) TextButton({vm.fotoPerfil(null)},enabled=!state.ocupado){Text("Quitar foto")}
        Campo("Nombre",nombre,{nombre=it})
        Campo("Correo",correo,{correo=it})
        Text("Rol: ${perfil.rol}")
        Accion("Guardar perfil",!state.ocupado){vm.guardarPerfil(nombre,correo)}
    }
}
