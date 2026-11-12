package pe.edu.upeu.solshiran.domain.model

data class Sesion(val token: String, val id: Long, val nombre: String, val email: String, val rol: String)
data class Cabana(
    val id: Long, val nombre: String, val descripcion: String, val capacidad: Int,
    val habitaciones: Int, val banos: Int, val tarifa: Double, val imagen: String,
    val fotos: List<String> = emptyList(), val activo: Boolean = true, val duenoId: Long? = null,
)
data class Cuenta(val id: Long, val nombre: String, val email: String, val rol: String,
    val activo: Boolean = true, val duenoId: Long? = null)
data class Reserva(val id: Long, val cabana: String, val ingreso: String, val salida: String,
    val estado: String, val total: Double, val adelanto: Double, val saldo: Double, val puedePagar: Boolean)
data class Consulta(val cabanaId: Long, val ingreso: String, val salida: String, val huespedes: Int)
data class Cotizacion(val consulta: Consulta, val disponible: Boolean, val noches: Int,
    val total: Double, val adelanto: Double, val mensaje: String)
data class Pago(val id: Long, val url: String, val monto: Double)
data class Opinion(val autor: String, val puntuacion: Int, val comentario: String)
data class Metricas(val ingresos: Double, val pagos: Int, val reservas: Int, val ranking: List<Pair<Long, Int>>)
data class FotoElegida(val bytes: ByteArray, val tipo: String, val nombre: String)
class ErrorApi(val estado: Int, override val message: String) : Exception(message)
