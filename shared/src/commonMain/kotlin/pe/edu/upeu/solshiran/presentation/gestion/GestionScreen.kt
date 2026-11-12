package pe.edu.upeu.solshiran.presentation.gestion

import androidx.compose.material3.*
import androidx.compose.runtime.*
import pe.edu.upeu.solshiran.domain.model.*
import pe.edu.upeu.solshiran.platform.*
import pe.edu.upeu.solshiran.presentation.*
import pe.edu.upeu.solshiran.presentation.components.*

@Composable fun CabanasGestionScreen(vm: AppViewModel,state: UiState,plataforma: Plataforma) {
    var editar by remember {mutableStateOf<Cabana?>(null)}
    var formulario by remember {mutableStateOf(false)}
    var confirmar by remember {mutableStateOf<Cabana?>(null)}
    Text("Gestionar cabañas",style=MaterialTheme.typography.headlineMedium)
    Accion("Nueva cabaña",!state.ocupado){editar=null;formulario=true}
    if(formulario) key(editar?.id) {
        var nombre by remember {mutableStateOf(editar?.nombre ?: "")}
        var descripcion by remember {mutableStateOf(editar?.descripcion ?: "")}
        var capacidad by remember {mutableStateOf((editar?.capacidad ?: 2).toString())}
        var habitaciones by remember {mutableStateOf((editar?.habitaciones ?: 1).toString())}
        var banos by remember {mutableStateOf((editar?.banos ?: 1).toString())}
        var tarifa by remember {mutableStateOf((editar?.tarifa ?: 100.0).toString())}
        var imagen by remember {mutableStateOf(editar?.imagen ?: "/images/cabana-placeholder.svg")}
        var fotos by remember {mutableStateOf(editar?.fotos ?: emptyList())}
        var dueno by remember {mutableStateOf(editar?.duenoId)}
        val duenos=state.cuentas.filter {it.rol=="DUENO" && it.activo}
        Tarjeta {
            Text(if(editar==null) "Nueva cabaña" else "Editar cabaña",style=MaterialTheme.typography.titleLarge)
            Campo("Nombre",nombre,{nombre=it})
            Campo("Descripción",descripcion,{descripcion=it})
            Campo("Capacidad",capacidad,{capacidad=it},numerico=true)
            Campo("Habitaciones",habitaciones,{habitaciones=it},numerico=true)
            Campo("Baños",banos,{banos=it},numerico=true)
            Campo("Tarifa por noche S/",tarifa,{tarifa=it},numerico=true)
            Opciones("Dueño", listOf("Administración") + duenos.map {"${it.id} · ${it.nombre}"},
                duenos.find {it.id==dueno}?.let {"${it.id} · ${it.nombre}"} ?: "Administración") {valor ->
                dueno=valor.substringBefore(" · ").toLongOrNull()
            }
            Foto(imagen,vm.repository)
            OutlinedButton({plataforma.elegirFoto {f -> if(f!=null) vm.subirFoto(f){imagen=it}}},enabled=!state.ocupado){Text("Seleccionar foto principal")}
            fotos.forEachIndexed {indice,url ->
                Foto(url,vm.repository)
                TextButton({fotos=fotos.filterIndexed {i,_->i!=indice}},enabled=!state.ocupado){Text("Quitar foto ${indice+1}")}
            }
            OutlinedButton({plataforma.elegirFoto {f -> if(f!=null) vm.subirFoto(f){fotos=fotos+it}}},enabled=!state.ocupado && fotos.size<8){Text("Añadir foto a la galería (${fotos.size}/8)")}
            Accion("Guardar cabaña",!state.ocupado){vm.guardarCabana(Cabana(editar?.id ?: 0,nombre,descripcion,
                capacidad.toIntOrNull() ?: 0,habitaciones.toIntOrNull() ?: 0,banos.toIntOrNull() ?: 0,
                tarifa.replace(',','.').toDoubleOrNull() ?: 0.0,imagen,fotos,duenoId=dueno)){formulario=false}}
            TextButton({formulario=false},enabled=!state.ocupado){Text("Cancelar")}
        }
    }
    state.gestion.forEach { c -> Tarjeta {
        Foto(c.imagen,vm.repository)
        Text(c.nombre,style=MaterialTheme.typography.titleLarge)
        Text("${dinero(c.tarifa)} · ${if(c.activo) "Activa" else "Deshabilitada"}")
        OutlinedButton({editar=c;formulario=true},enabled=!state.ocupado){Text("Editar")}
        TextButton({confirmar=c},enabled=!state.ocupado){Text(if(c.activo) "Deshabilitar" else "Activar")}
    } }
    confirmar?.let {c -> Confirmar("Cambiar estado", "¿${if(c.activo) "Deshabilitar" else "Activar"} ${c.nombre}? Se conservará su historial.", {confirmar=null}){vm.cambiarCabana(c)} }
}

@Composable fun CuentasScreen(vm: AppViewModel,state: UiState) {
    var editar by remember {mutableStateOf<Cuenta?>(null)}
    var formulario by remember {mutableStateOf(false)}
    var eliminar by remember {mutableStateOf<Cuenta?>(null)}
    var filtro by remember {mutableStateOf("TODOS")}
    val sesion by vm.sesion.collectAsState()
    Text("Cuentas y permisos",style=MaterialTheme.typography.headlineMedium)
    Opciones("Listado",listOf("TODOS","CLIENTE","TRABAJADOR","DUENO","ADMIN"),filtro){filtro=it}
    Accion("Crear cuenta",!state.ocupado){editar=null;formulario=true}
    if(formulario) key(editar?.id) {
        var nombre by remember {mutableStateOf(editar?.nombre ?: "")}
        var email by remember {mutableStateOf(editar?.email ?: "")}
        var password by remember {mutableStateOf("")}
        var rol by remember {mutableStateOf(editar?.rol ?: "CLIENTE")}
        var dueno by remember {mutableStateOf(editar?.duenoId)}
        val duenos=state.cuentas.filter {it.rol=="DUENO" && it.activo}
        Tarjeta {
            Text(if(editar==null) "Nueva cuenta" else "Editar cuenta",style=MaterialTheme.typography.titleLarge)
            Campo("Nombre",nombre,{nombre=it})
            Campo("Correo",email,{email=it})
            if(editar==null) Campo("Contraseña inicial",password,{password=it},secreto=true)
            if(editar?.id!=sesion?.id) Opciones("Rol",listOf("CLIENTE","TRABAJADOR","DUENO","ADMIN"),rol){rol=it;dueno=null}
            if(rol=="TRABAJADOR") Opciones("Dueño",listOf("Administración")+duenos.map {"${it.id} · ${it.nombre}"},
                duenos.find {it.id==dueno}?.let {"${it.id} · ${it.nombre}"} ?: "Administración") {dueno=it.substringBefore(" · ").toLongOrNull()}
            Accion("Guardar cuenta",!state.ocupado){vm.guardarCuenta(Cuenta(editar?.id ?: 0,nombre,email,rol,duenoId=dueno),password){formulario=false}}
            TextButton({formulario=false},enabled=!state.ocupado){Text("Cancelar")}
        }
    }
    state.cuentas.filter {filtro=="TODOS" || it.rol==filtro}.forEach { c -> Tarjeta {
        Text(c.nombre,style=MaterialTheme.typography.titleMedium)
        Text(c.email)
        Text("${c.rol} · ${if(c.activo) "Activo" else "Inactivo"}")
        OutlinedButton({editar=c;formulario=true},enabled=!state.ocupado){Text("Editar")}
        if(c.id!=sesion?.id) TextButton({eliminar=c},enabled=!state.ocupado){Text(if(c.rol=="ADMIN") "Desactivar" else "Eliminar")}
    } }
    eliminar?.let {c -> Confirmar(if(c.rol=="ADMIN") "Desactivar cuenta" else "Eliminar definitivamente",
        "¿Continuar con ${c.email}? ${if(c.rol=="ADMIN") "La cuenta quedará desactivada." else "La cuenta se eliminará y no podrás recuperarla."}",
        {eliminar=null}){vm.eliminarCuenta(c)} }
}

@Composable fun MetricasScreen(vm: AppViewModel,state: UiState) {
    var desde by remember {mutableStateOf(hoy().take(7)+"-01")}
    var hasta by remember {mutableStateOf(hoy())}
    Text("Métricas de ventas",style=MaterialTheme.typography.headlineMedium)
    Fecha("Desde",desde){desde=it}
    Fecha("Hasta",hasta){hasta=it}
    Accion("Consultar reporte",!state.ocupado){vm.metricas(desde,hasta)}
    state.metricas?.let { m ->
        Tarjeta {
            Text("Ingresos aprobados",style=MaterialTheme.typography.titleLarge)
            Text(dinero(m.ingresos),style=MaterialTheme.typography.headlineMedium)
            Text("${m.pagos} pagos · ${m.reservas} reservas")
        }
        Text("Frecuencia por cabaña",style=MaterialTheme.typography.titleLarge)
        m.ranking.forEach {(id,cantidad) -> Tarjeta {Text("Cabaña #$id · $cantidad reservas")} }
    }
}
