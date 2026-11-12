package pe.edu.upeu.solshiran.presentation.catalogo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.edu.upeu.solshiran.domain.model.*
import pe.edu.upeu.solshiran.presentation.*
import pe.edu.upeu.solshiran.presentation.components.*
import pe.edu.upeu.solshiran.platform.hoy
import pe.edu.upeu.solshiran.platform.dentroDeUnAno
import pe.edu.upeu.solshiran.domain.usecase.rangoLibre

@Composable fun CatalogoScreen(vm: AppViewModel,state: UiState) {
    var buscar by remember {mutableStateOf("")}
    Text("Encuentra tu espacio para descansar",style=MaterialTheme.typography.headlineLarge)
    Text("Cabañas, naturaleza y bienestar en Sol de Shirán.")
    Campo("Buscar cabaña",buscar,{buscar=it})
    OutlinedButton({vm.catalogo()},enabled=!state.ocupado) {Text("Actualizar catálogo")}
    if(state.offline) Text("Sin conexión · Mostrando catálogo guardado",color=MaterialTheme.colorScheme.error)
    val cabanas=state.cabanas.filter {it.nombre.contains(buscar,true)}
    if(cabanas.isEmpty() && !state.ocupado) Text("No hay cabañas disponibles para mostrar.")
    cabanas.forEach { c ->
        Tarjeta {
            Box(Modifier.clickable(enabled=!state.ocupado) {vm.ver(c)}) {Foto(c.imagen,vm.repository)}
            Text(c.nombre,style=MaterialTheme.typography.titleLarge)
            Text("Hasta ${c.capacidad} huéspedes · ${c.habitaciones} habitaciones · ${c.banos} baños")
            Text("${dinero(c.tarifa)} por noche",style=MaterialTheme.typography.titleMedium)
            Accion("Ver cabaña",!state.ocupado){vm.ver(c)}
        }
    }
}
@Composable fun DetalleScreen(vm: AppViewModel,state: UiState) {
    val c=state.detalle ?: return
    val sesion by vm.sesion.collectAsState()
    var ingreso by remember(c.id) {mutableStateOf("")}
    var salida by remember(c.id) {mutableStateOf("")}
    var huespedes by remember(c.id) {mutableStateOf("2")}
    var estrellas by remember(c.id) {mutableStateOf("5")}
    var comentario by remember(c.id) {mutableStateOf("")}
    Text(c.nombre,style=MaterialTheme.typography.headlineMedium)
    Foto(c.imagen,vm.repository)
    c.fotos.forEach { Foto(it,vm.repository,Modifier.fillMaxWidth().height(160.dp)) }
    Text(c.descripcion)
    Text("Hasta ${c.capacidad} huéspedes · ${c.habitaciones} habitaciones · ${c.banos} baños")
    Text("${dinero(c.tarifa)} por noche",style=MaterialTheme.typography.titleLarge)
    Tarjeta {
        Text("Consulta tu estancia",style=MaterialTheme.typography.titleLarge)
        OutlinedButton({vm.actualizarFechas()},enabled=!state.ocupado){Text("Actualizar disponibilidad")}
        if(!state.calendarioCargado) Text("Consulta las fechas ocupadas para seleccionar tu estancia.")
        else Text("Las noches ocupadas están deshabilitadas. Puedes elegir fechas hasta ${dentroDeUnAno()}.")
        Fecha("Ingreso",ingreso,habilitado=state.calendarioCargado && !state.ocupado,
            permitida={it>=hoy() && it<=dentroDeUnAno() && it !in state.fechasOcupadas}) {
            ingreso=it;salida="";vm.invalidarCotizacion()
        }
        Fecha("Salida",salida,habilitado=state.calendarioCargado && ingreso.isNotBlank() && !state.ocupado,
            permitida={it<=dentroDeUnAno() && rangoLibre(ingreso,it,state.fechasOcupadas)}) {
            salida=it;vm.invalidarCotizacion()
        }
        Campo("Huéspedes",huespedes,{huespedes=it;vm.invalidarCotizacion()},numerico=true)
        val consulta=Consulta(c.id,ingreso,salida,huespedes.toIntOrNull() ?: 0)
        Accion("Consultar disponibilidad",!state.ocupado && state.calendarioCargado && rangoLibre(ingreso,salida,state.fechasOcupadas)) {vm.cotizar(consulta)}
        state.cotizacion?.takeIf {it.consulta==consulta}?.let { cot ->
            Text(cot.mensaje)
            if(cot.disponible) {
                Text("${cot.noches} noches · Total ${dinero(cot.total)}")
                Text("Adelanto del 50 %: ${dinero(cot.adelanto)}")
                Text("El servidor vuelve a comprobar la disponibilidad al reservar.")
                if(sesion?.rol=="CLIENTE") Accion("Crear reserva",!state.ocupado){vm.reservar()}
                else TextButton({vm.ir("login")}){Text("Inicia sesión como cliente para reservar")}
            }
        }
    }
    Text("Opiniones de los huéspedes",style=MaterialTheme.typography.titleLarge)
    if(state.opiniones.isEmpty()) Text("No hay opiniones para mostrar.")
    state.opiniones.forEach { o -> Tarjeta {Text("${o.autor} · ${o.puntuacion}/5");Text(o.comentario)} }
    if(sesion?.rol=="CLIENTE") Tarjeta {
        Text("Valora una estancia finalizada")
        Opciones("Estrellas",listOf("1","2","3","4","5"),estrellas){estrellas=it}
        Campo("Tu comentario",comentario,{comentario=it})
        Accion("Publicar reseña",!state.ocupado){vm.opinar(estrellas.toInt(),comentario)}
    }
}
