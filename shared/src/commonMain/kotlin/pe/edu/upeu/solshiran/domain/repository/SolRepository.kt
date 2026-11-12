package pe.edu.upeu.solshiran.domain.repository

import kotlinx.coroutines.flow.StateFlow
import pe.edu.upeu.solshiran.domain.model.*

interface AlmacenSesion {
    fun leer(): Sesion?
    fun guardar(sesion: Sesion?)
}
interface SolRepository {
    val sesion: StateFlow<Sesion?>
    val urlBase: String
    suspend fun entrar(email: String, password: String)
    suspend fun registrar(nombre: String, email: String, password: String)
    fun salir()
    suspend fun catalogo(): List<Cabana>
    suspend fun catalogoLocal(): List<Cabana>
    suspend fun detalle(id: Long): Cabana
    suspend fun cotizar(consulta: Consulta): Cotizacion
    suspend fun fechasOcupadas(idCabana: Long, desde: String, hasta: String): Set<String>
    suspend fun reservar(cotizacion: Cotizacion): Long
    suspend fun reservas(pagina: Int = 0): List<Reserva>
    suspend fun pagar(idReserva: Long): Pago
    suspend fun estadoPago(id: Long): String
    suspend fun perfil(): Cuenta
    suspend fun guardarPerfil(nombre: String, email: String)
    suspend fun fotoPerfil(): ByteArray?
    suspend fun guardarFotoPerfil(foto: FotoElegida)
    suspend fun quitarFotoPerfil()
    suspend fun imagen(url: String): ByteArray
    suspend fun cabanasGestion(): List<Cabana>
    suspend fun guardarCabana(cabana: Cabana)
    suspend fun desactivarCabana(id: Long)
    suspend fun activarCabana(id: Long)
    suspend fun subirFoto(foto: FotoElegida): String
    suspend fun cuentas(): List<Cuenta>
    suspend fun guardarCuenta(cuenta: Cuenta, password: String)
    suspend fun eliminarCuenta(id: Long)
    suspend fun checkin(id: Long)
    suspend fun cobrarSaldo(id: Long, medio: String, operacion: String, clave: String)
    suspend fun checkout(id: Long, descripcion: String, monto: Double, medio: String)
    suspend fun opiniones(idCabana: Long): List<Opinion>
    suspend fun publicarOpinion(idCabana: Long, puntuacion: Int, comentario: String)
    suspend fun metricas(desde: String, hasta: String): Metricas
}
