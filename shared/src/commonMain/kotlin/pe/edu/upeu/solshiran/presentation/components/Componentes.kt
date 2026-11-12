@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package pe.edu.upeu.solshiran.presentation.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import pe.edu.upeu.solshiran.domain.repository.SolRepository
import pe.edu.upeu.solshiran.platform.*
import kotlinx.coroutines.CancellationException

@Composable fun Campo(titulo: String,valor: String,cambiar: (String)->Unit,
    secreto: Boolean=false,numerico: Boolean=false) {
    OutlinedTextField(valor,cambiar,Modifier.fillMaxWidth(),label={Text(titulo)},
        singleLine=true,visualTransformation=if(secreto) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(
            keyboardType=if(secreto) KeyboardType.Password else if(numerico) KeyboardType.Decimal else KeyboardType.Text))
}
@Composable fun Accion(texto: String, habilitado: Boolean=true, hacer: ()->Unit) {
    Button(hacer,Modifier.fillMaxWidth().heightIn(min=48.dp),enabled=habilitado,
        shape=RoundedCornerShape(12.dp)) { Text(texto) }
}
@Composable fun Tarjeta(contenido: @Composable ColumnScope.()->Unit) {
    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(
        containerColor=MaterialTheme.colorScheme.surface,
        contentColor=MaterialTheme.colorScheme.onSurface)) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=contenido)
    }
}
@Composable fun Foto(url: String, repository: SolRepository,modifier: Modifier=Modifier.fillMaxWidth().height(190.dp)) {
    val bytes by produceState<ByteArray?>(null,url) {
        value = try { repository.imagen(url) } catch(e: CancellationException) { throw e } catch(_: Exception) { null }
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        val imagen=bytes
        if(imagen!=null) ImagenBytes(imagen,"Foto de la cabaña",Modifier.fillMaxSize())
        else Text("Sol de Shirán · Naturaleza y descanso",Modifier.padding(20.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable fun Fecha(etiqueta: String,valor: String,habilitado: Boolean=true,
    permitida: (String)->Boolean = { true }, cambiar: (String)->Unit) {
    var mostrar by remember { mutableStateOf(false) }
    OutlinedButton({mostrar=true},Modifier.fillMaxWidth(),enabled=habilitado) { Text("$etiqueta: ${valor.ifBlank { "Seleccionar fecha" }}") }
    if(mostrar) {
        val regla = rememberUpdatedState(permitida)
        val seleccionables = remember { object: SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = regla.value(fechaIso(utcTimeMillis))
        } }
        val picker=rememberDatePickerState(selectableDates=seleccionables)
        DatePickerDialog(onDismissRequest={mostrar=false},confirmButton={
            TextButton({picker.selectedDateMillis?.let {cambiar(fechaIso(it))};mostrar=false},enabled=picker.selectedDateMillis!=null) {Text("Seleccionar")}
        },dismissButton={TextButton({mostrar=false}){Text("Cancelar")}}) { DatePicker(picker) }
    }
}
@Composable fun Opciones(etiqueta: String, opciones: List<String>,seleccionado: String,cambiar: (String)->Unit) {
    var abrir by remember { mutableStateOf(false) }
    Column {
        OutlinedButton({abrir=true},Modifier.fillMaxWidth()) {Text("$etiqueta: $seleccionado")}
        DropdownMenu(abrir,{abrir=false}) { opciones.forEach { opcion -> DropdownMenuItem({Text(opcion)}, {cambiar(opcion);abrir=false}) } }
    }
}
@Composable fun Confirmar(titulo: String,texto: String,cerrar: ()->Unit,confirmar: ()->Unit) {
    AlertDialog(onDismissRequest=cerrar,title={Text(titulo)},text={Text(texto)},
        confirmButton={TextButton({confirmar();cerrar()}){Text("Confirmar")}},
        dismissButton={TextButton(cerrar){Text("Cancelar")}})
}
fun dinero(valor: Double): String {
    val centimos=kotlin.math.round(valor*100).toLong()
    return "S/ ${centimos/100}.${(centimos%100).toString().padStart(2,'0')}"
}
