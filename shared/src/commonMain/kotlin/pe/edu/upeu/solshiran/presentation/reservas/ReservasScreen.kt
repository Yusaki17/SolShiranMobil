package pe.edu.upeu.solshiran.presentation.reservas

import androidx.compose.material3.*
import androidx.compose.runtime.*
import pe.edu.upeu.solshiran.domain.model.Reserva
import pe.edu.upeu.solshiran.platform.Plataforma
import pe.edu.upeu.solshiran.platform.nuevoId
import pe.edu.upeu.solshiran.platform.hoy
import pe.edu.upeu.solshiran.domain.usecase.puedeIniciarEstancia
import pe.edu.upeu.solshiran.presentation.*
import pe.edu.upeu.solshiran.presentation.components.*

@Composable fun ReservasScreen(vm: AppViewModel,state: UiState,plataforma: Plataforma) {
    val sesion by vm.sesion.collectAsState()
    val operativo=sesion?.rol in listOf("ADMIN","TRABAJADOR")
    var salida by remember {mutableStateOf<Reserva?>(null)}
    var descripcion by remember {mutableStateOf("")}
    var monto by remember {mutableStateOf("")}
    var medio by remember {mutableStateOf("EFECTIVO")}
    var cobro by remember {mutableStateOf<Reserva?>(null)}
    var medioSaldo by remember {mutableStateOf("EFECTIVO")}
    var operacion by remember {mutableStateOf("")}
    var claveCobro by remember {mutableStateOf("")}
    Text(if(operativo) "Panel de reservas" else "Mis reservas",style=MaterialTheme.typography.headlineMedium)
    OutlinedButton({vm.cargarReservas(state.paginaReservas)},enabled=!state.ocupado){Text("Actualizar reservas")}
    state.pago?.let { p -> Tarjeta {
        Text("Transacción #${p.id} · Adelanto ${dinero(p.monto)}")
        Accion("Consultar estado del pago",!state.ocupado){vm.consultarPago()}
        TextButton({plataforma.abrirPago(p.url)}){Text("Volver a Mercado Pago")}
    } }
    if(state.reservas.isEmpty() && !state.ocupado) Text("No hay reservas en esta página.")
    state.reservas.forEach { r -> Tarjeta {
        Text("${r.cabana} · #${r.id}",style=MaterialTheme.typography.titleLarge)
        Text("${r.ingreso} → ${r.salida}")
        Text(r.estado)
        Text("Total ${dinero(r.total)} · Adelanto ${dinero(r.adelanto)} · Saldo ${dinero(r.saldo)}")
        if(sesion?.rol=="CLIENTE" && r.puedePagar) Accion("Pagar adelanto del 50 %",!state.ocupado){vm.pagar(r.id,plataforma::abrirPago)}
        if(operativo && r.estado=="CONFIRMADA") {
            val puedeCobrar=puedeIniciarEstancia(r.estado,r.ingreso,r.salida,hoy())
            if(!puedeCobrar) Text(if(hoy()<r.ingreso) "Pago del saldo y check-in disponibles desde ${r.ingreso}." else "La fecha de salida ya pasó. No se puede iniciar la estancia.")
            if(r.saldo>0) Accion("Registrar pago del saldo",!state.ocupado && puedeCobrar){cobro=r;operacion="";claveCobro=nuevoId()}
            else Accion("Registrar check-in",!state.ocupado && puedeCobrar){vm.checkin(r.id)}
        }
        if(operativo && r.estado=="OCUPADA") OutlinedButton({salida=r;descripcion="";monto=""},enabled=!state.ocupado){Text("Registrar check-out")}
    } }
    if(state.paginaReservas>0) TextButton({vm.cargarReservas(state.paginaReservas-1)},enabled=!state.ocupado){Text("Página anterior")}
    if(state.reservas.size==20) TextButton({vm.cargarReservas(state.paginaReservas+1)},enabled=!state.ocupado){Text("Siguiente página")}
    cobro?.let { r -> AlertDialog(onDismissRequest={if(!state.ocupado)cobro=null},title={Text("Cobrar saldo ${dinero(r.saldo)}")},
        text={androidx.compose.foundation.layout.Column {
            Text("Registra este pago únicamente después de haber recibido el dinero.")
            Opciones("Medio de pago",listOf("EFECTIVO","TARJETA","YAPE_BILLETERA"),medioSaldo){medioSaldo=it}
            Campo("Número de operación (si corresponde)",operacion,{operacion=it})
        }},confirmButton={TextButton({vm.cobrarSaldo(r.id,medioSaldo,operacion,claveCobro){cobro=null}},enabled=!state.ocupado){Text("Confirmar cobro recibido")}},
        dismissButton={TextButton({cobro=null},enabled=!state.ocupado){Text("Cancelar")}}) }
    salida?.let { r -> AlertDialog(onDismissRequest={salida=null},title={Text("Salida de ${r.cabana}")},text={
        androidx.compose.foundation.layout.Column {
            Text("Deja los daños vacíos si no corresponde un cobro adicional.")
            Campo("Descripción de daños",descripcion,{descripcion=it})
            Campo("Importe S/",monto,{monto=it},numerico=true)
            Opciones("Medio de pago",listOf("EFECTIVO","TARJETA","YAPE","PLIN"),medio){medio=it}
        }
    },confirmButton={TextButton({vm.checkout(r.id,descripcion,monto.toDoubleOrNull() ?: 0.0,medio);salida=null}){Text("Registrar salida")}},
        dismissButton={TextButton({salida=null}){Text("Cancelar")}}) }
}
