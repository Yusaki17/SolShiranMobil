@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package pe.edu.upeu.solshiran.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import pe.edu.upeu.solshiran.platform.Plataforma
import pe.edu.upeu.solshiran.presentation.auth.LoginScreen
import pe.edu.upeu.solshiran.presentation.catalogo.*
import pe.edu.upeu.solshiran.presentation.perfil.PerfilScreen
import pe.edu.upeu.solshiran.presentation.reservas.ReservasScreen
import pe.edu.upeu.solshiran.presentation.gestion.*

@Composable fun SolShiranApp(vm: AppViewModel,plataforma: Plataforma) {
    val state by vm.state.collectAsState()
    val sesion by vm.sesion.collectAsState()
    val sistemaOscuro = isSystemInDarkTheme()
    var oscuro by rememberSaveable { mutableStateOf(plataforma.leerModoOscuro() ?: sistemaOscuro) }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val opciones = buildList {
        add("catalogo" to "Inicio · Catálogo")
        if (sesion == null) { add("login" to "Iniciar sesión"); add("registro" to "Registrarme") }
        if (sesion?.rol in listOf("CLIENTE", "ADMIN", "TRABAJADOR")) add("reservas" to "Reservas")
        if (sesion?.rol == "ADMIN") {
            add("cabanas" to "Gestionar cabañas"); add("cuentas" to "Usuarios y roles"); add("metricas" to "Reportes")
        }
        if (sesion != null) add("perfil" to "Mi perfil")
    }
    val colores = if (oscuro) darkColorScheme(
        primary=Color(0xFFA0D5AC), onPrimary=Color(0xFF103820),
        secondary=Color(0xFFFFB865), onSecondary=Color(0xFF492900),
        background=Color(0xFF101813), onBackground=Color(0xFFE0E9DF),
        surface=Color(0xFF17221B), onSurface=Color(0xFFE0E9DF),
        surfaceVariant=Color(0xFF2D3B30), onSurfaceVariant=Color(0xFFBFCDBE)
    ) else lightColorScheme(
        primary=Color(0xFF245439), onPrimary=Color.White,
        secondary=Color(0xFFFF9818), onSecondary=Color(0xFF213E2B),
        background=Color(0xFFF5EFE3), onBackground=Color(0xFF213E2B),
        surface=Color(0xFFFFFBF4), onSurface=Color(0xFF213E2B)
    )
    MaterialTheme(colorScheme=colores) {
        ModalNavigationDrawer(drawerState=drawer, drawerContent={
            ModalDrawerSheet(Modifier.widthIn(max=320.dp).fillMaxHeight()) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Text("Sol de Shirán", style=MaterialTheme.typography.headlineSmall,
                        color=MaterialTheme.colorScheme.primary, modifier=Modifier.padding(vertical=16.dp))
                    Text(sesion?.nombre ?: "Bienvenido", style=MaterialTheme.typography.titleMedium)
                    Text(sesion?.let { "${it.email} · ${it.rol}" } ?: "Descanso y naturaleza",
                        style=MaterialTheme.typography.bodySmall)
                    HorizontalDivider(Modifier.padding(vertical=20.dp))
                    opciones.forEach { (pagina, nombre) ->
                        NavigationDrawerItem(
                            label={Text(nombre)}, selected=state.pagina==pagina,
                            onClick={ if(!state.ocupado) { vm.ir(pagina); scope.launch { drawer.close() } } },
                            modifier=Modifier.padding(vertical=4.dp)
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    if(sesion!=null) TextButton(onClick={
                        if(!state.ocupado) { vm.salir(); scope.launch { drawer.close() } }
                    }) { Text("Cerrar sesión", color=MaterialTheme.colorScheme.error) }
                    HorizontalDivider(Modifier.padding(vertical=12.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.SpaceBetween) {
                        Text("Modo oscuro")
                        Switch(checked=oscuro, onCheckedChange={
                            oscuro=it; plataforma.guardarModoOscuro(it)
                        })
                    }
                }
            }
        }) {
        Scaffold(topBar={TopAppBar(title={Text(opciones.firstOrNull { it.first==state.pagina }?.second ?: "Sol de Shirán")},navigationIcon={
            TextButton(onClick={scope.launch { drawer.open() }}) {
                Text("☰",style=MaterialTheme.typography.headlineSmall)
            }
        })}) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement=Arrangement.spacedBy(16.dp)) {
                sesion?.let {Text("${it.nombre} · ${it.rol}",style=MaterialTheme.typography.labelLarge)}
                if(state.ocupado) LinearProgressIndicator(Modifier.fillMaxWidth())
                if(state.error.isNotBlank()) Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.errorContainer)) {
                    Text(state.error,Modifier.padding(14.dp))
                }
                if(state.mensaje.isNotBlank()) Text(state.mensaje,color=MaterialTheme.colorScheme.primary)
                when(state.pagina) {
                    "login", "registro" -> LoginScreen(vm,state.pagina=="registro")
                    "detalle" -> DetalleScreen(vm,state)
                    "perfil" -> if(sesion!=null) PerfilScreen(vm,state,plataforma)
                    "reservas" -> if(sesion!=null) ReservasScreen(vm,state,plataforma)
                    "cabanas" -> if(sesion?.rol=="ADMIN") CabanasGestionScreen(vm,state,plataforma)
                    "cuentas" -> if(sesion?.rol=="ADMIN") CuentasScreen(vm,state)
                    "metricas" -> if(sesion?.rol=="ADMIN") MetricasScreen(vm,state)
                    else -> CatalogoScreen(vm,state)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
}
