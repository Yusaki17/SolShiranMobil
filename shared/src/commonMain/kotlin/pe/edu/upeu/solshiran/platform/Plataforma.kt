package pe.edu.upeu.solshiran.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pe.edu.upeu.solshiran.domain.model.FotoElegida

interface Plataforma {
    fun leerModoOscuro(): Boolean? = null
    fun guardarModoOscuro(oscuro: Boolean) {}
    fun abrirPago(url: String)
    fun elegirFoto(resultado: (FotoElegida?) -> Unit)
}
expect fun tokenVigente(token: String): Boolean
expect fun fechaIso(milisegundos: Long): String
expect fun hoy(): String
expect fun dentroDeUnAno(): String
expect fun nuevoId(): String
@Composable expect fun ImagenBytes(bytes: ByteArray, descripcion: String, modifier: Modifier)
