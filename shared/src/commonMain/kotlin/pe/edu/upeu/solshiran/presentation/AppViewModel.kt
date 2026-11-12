package pe.edu.upeu.solshiran.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import pe.edu.upeu.solshiran.domain.model.*
import pe.edu.upeu.solshiran.domain.repository.SolRepository
import pe.edu.upeu.solshiran.domain.usecase.ValidarReserva
import pe.edu.upeu.solshiran.platform.hoy
import pe.edu.upeu.solshiran.platform.dentroDeUnAno

data class UiState(
    val ocupado: Boolean = false, val error: String = "", val mensaje: String = "", val offline: Boolean = false,
    val pagina: String = "catalogo", val cabanas: List<Cabana> = emptyList(), val detalle: Cabana? = null,
    val cotizacion: Cotizacion? = null, val reservas: List<Reserva> = emptyList(), val paginaReservas: Int = 0,
    val pago: Pago? = null, val perfil: Cuenta? = null, val fotoPerfil: ByteArray? = null,
    val gestion: List<Cabana> = emptyList(), val cuentas: List<Cuenta> = emptyList(),
    val opiniones: List<Opinion> = emptyList(), val metricas: Metricas? = null,
    val fechasOcupadas: Set<String> = emptySet(), val calendarioCargado: Boolean = false,
)
class AppViewModel(val repository: SolRepository) : ViewModel() {
    val sesion = repository.sesion
    private val mutable = MutableStateFlow(UiState())
    val state = mutable.asStateFlow()
    private var versionCotizacion = 0
    init {
        catalogo()
        viewModelScope.launch {
            var anterior: Long? = sesion.value?.id
            sesion.collect { s ->
                if (s?.id != anterior) {
                    mutable.value = UiState(cabanas=mutable.value.cabanas)
                    anterior = s?.id
                }
            }
        }
    }
    fun mensaje(texto: String) { mutable.update { it.copy(mensaje=texto,error="") } }
    fun error(texto: String) { mutable.update { it.copy(error=texto,mensaje="") } }
    private fun ejecutar(bloque: suspend () -> Unit) {
        if (mutable.value.ocupado) return
        mutable.update { it.copy(ocupado=true,error="",mensaje="") }
        viewModelScope.launch {
            try { bloque() }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) {
                mutable.update { it.copy(error=if(e is ErrorApi || e is IllegalArgumentException) e.message ?: "Revisa los datos."
                    else "No se pudo conectar. Comprueba el gateway y la red.") }
            } finally { mutable.update { it.copy(ocupado=false) } }
        }
    }
    fun ir(pagina: String) {
        if (state.value.ocupado) return
        mutable.update { it.copy(pagina=pagina,detalle=null,cotizacion=null,error="",mensaje="") }
        when(pagina) {
            "catalogo" -> catalogo()
            "reservas" -> cargarReservas()
            "perfil" -> cargarPerfil()
            "cabanas" -> ejecutar { mutable.update { it.copy(gestion=repository.cabanasGestion(),cuentas=repository.cuentas()) } }
            "cuentas" -> ejecutar { mutable.update { it.copy(cuentas=repository.cuentas()) } }
        }
    }
    fun catalogo() = ejecutar {
        val local = repository.catalogoLocal()
        if(local.isNotEmpty()) mutable.update { it.copy(cabanas=local) }
        try { val remoto=repository.catalogo(); mutable.update { it.copy(cabanas=remoto,offline=false) } }
        catch(e: CancellationException) { throw e }
        catch(e: Exception) {
            if (local.isEmpty()) throw e
            mutable.update { it.copy(offline=true,mensaje="Catálogo guardado en el dispositivo. Para reservar necesitas conexión.") }
        }
    }
    fun entrar(email: String, password: String) = ejecutar {
        repository.entrar(email,password)
        mutable.update { it.copy(pagina="catalogo",mensaje="Sesión iniciada.") }
    }
    fun registrar(nombre: String, email: String, password: String) = ejecutar {
        repository.registrar(nombre,email,password)
        mutable.update { it.copy(pagina="login",mensaje="Cuenta creada. Inicia sesión.") }
    }
    fun salir() { repository.salir(); mutable.value=UiState(cabanas=state.value.cabanas) }
    fun ver(cabana: Cabana) = ejecutar {
        mutable.update { it.copy(detalle=cabana,pagina="detalle",cotizacion=null,opiniones=emptyList(),fechasOcupadas=emptySet(),calendarioCargado=false) }
        try { val detalle=repository.detalle(cabana.id); mutable.update { it.copy(detalle=detalle) } }
        catch(e: CancellationException) { throw e }
        catch(e: Exception) { if(!state.value.offline) throw e }
        cargarFechasInterno(cabana.id)
        try { val opiniones=repository.opiniones(cabana.id); mutable.update { it.copy(opiniones=opiniones) } }
        catch(e: CancellationException) { throw e }
        catch(_: Exception) { mensaje("No se pudieron cargar las reseñas. Puedes reintentar abriendo la ficha.") }
    }
    private suspend fun cargarFechasInterno(id: Long) {
        val fechas = repository.fechasOcupadas(id,hoy(),dentroDeUnAno())
        mutable.update { it.copy(fechasOcupadas=fechas,calendarioCargado=true) }
    }
    fun actualizarFechas() = ejecutar {
        val id=state.value.detalle?.id ?: return@ejecutar
        invalidarCotizacion()
        mutable.update { it.copy(calendarioCargado=false) }
        cargarFechasInterno(id)
    }
    fun invalidarCotizacion() { versionCotizacion++; mutable.update { it.copy(cotizacion=null) } }
    fun cotizar(consulta: Consulta) = ejecutar {
        val version = versionCotizacion
        ValidarReserva()(consulta,hoy(),state.value.detalle?.capacidad ?: 0)
        val resultado=repository.cotizar(consulta)
        if(version==versionCotizacion) mutable.update { it.copy(cotizacion=resultado) }
    }
    fun reservar() = ejecutar {
        val c = state.value.cotizacion ?: throw IllegalArgumentException("Consulta la disponibilidad primero.")
        require(c.disponible) { "La cabaña no está disponible para esas fechas." }
        require(sesion.value?.rol == "CLIENTE") { "Inicia sesión con una cuenta de cliente para reservar." }
        val id = try { repository.reservar(c) }
        catch(e: ErrorApi) {
            if(e.estado==409) {
                invalidarCotizacion()
                mutable.update { it.copy(calendarioCargado=false) }
                try { cargarFechasInterno(c.consulta.cabanaId) }
                catch(cancelacion: CancellationException) { throw cancelacion }
                catch(_: Exception) { /* Sin calendario confirmado no se permite seleccionar otra estancia. */ }
            }
            throw e
        }
        mutable.update { it.copy(cotizacion=null,pagina="reservas",detalle=null,mensaje="Reserva #$id creada. Puedes pagar el adelanto del 50 %.") }
        cargarReservasInterno()
    }
    private suspend fun cargarReservasInterno(pagina: Int=0) {
        val r = repository.reservas(pagina)
        mutable.update { it.copy(reservas=r,paginaReservas=pagina) }
    }
    fun cargarReservas(pagina: Int=0) = ejecutar { cargarReservasInterno(pagina) }
    fun pagar(id: Long, abrir: (String) -> Unit) = ejecutar {
        val pago=repository.pagar(id)
        require(pago.url.startsWith("https://")) { "La pasarela devolvió un enlace inválido." }
        mutable.update { it.copy(pago=pago,mensaje="Completa el pago en Mercado Pago y después consulta su estado aquí.") }
        abrir(pago.url)
    }
    fun consultarPago() = ejecutar {
        val pago = state.value.pago ?: return@ejecutar
        val estado = repository.estadoPago(pago.id)
        mutable.update { it.copy(mensaje="Estado del pago: $estado. El servidor confirma el resultado de la pasarela.") }
        cargarReservasInterno()
    }
    private suspend fun perfilInterno() {
        val p=repository.perfil(); val foto=repository.fotoPerfil()
        mutable.update { it.copy(perfil=p,fotoPerfil=foto) }
    }
    fun cargarPerfil() = ejecutar { perfilInterno() }
    fun guardarPerfil(nombre: String,email: String) = ejecutar {
        repository.guardarPerfil(nombre,email); perfilInterno(); mensaje("Perfil guardado.")
    }
    fun fotoPerfil(foto: FotoElegida?) = ejecutar {
        if(foto==null) repository.quitarFotoPerfil() else repository.guardarFotoPerfil(foto)
        perfilInterno(); mensaje("Foto de perfil actualizada.")
    }
    fun subirFoto(foto: FotoElegida, completar: (String) -> Unit) = ejecutar { completar(repository.subirFoto(foto)) }
    fun guardarCabana(c: Cabana, completar: () -> Unit) = ejecutar {
        repository.guardarCabana(c); mutable.update { it.copy(gestion=repository.cabanasGestion()) }; completar(); mensaje("Cabaña guardada.")
    }
    fun cambiarCabana(c: Cabana) = ejecutar {
        if(c.activo) repository.desactivarCabana(c.id) else repository.activarCabana(c.id)
        mutable.update { it.copy(gestion=repository.cabanasGestion()) }
    }
    fun guardarCuenta(c: Cuenta,password: String, completar: () -> Unit) = ejecutar {
        repository.guardarCuenta(c,password); mutable.update { it.copy(cuentas=repository.cuentas()) }; completar(); mensaje("Cuenta guardada.")
    }
    fun eliminarCuenta(c: Cuenta) = ejecutar { repository.eliminarCuenta(c.id); mutable.update { it.copy(cuentas=repository.cuentas()) }; mensaje("Cuenta actualizada.") }
    fun checkin(id: Long) = ejecutar { repository.checkin(id); cargarReservasInterno(); mensaje("Ingreso registrado.") }
    fun cobrarSaldo(id: Long, medio: String, operacion: String, clave: String, completar: () -> Unit) = ejecutar {
        repository.cobrarSaldo(id,medio,operacion,clave); cargarReservasInterno(); completar(); mensaje("Pago del saldo registrado. Consulta el estado de la reserva.")
    }
    fun checkout(id: Long,descripcion: String,monto: Double,medio: String) = ejecutar {
        require(descripcion.isBlank() || monto > 0) { "Ingresa el importe de los daños." }
        repository.checkout(id,descripcion,monto,medio); cargarReservasInterno(); mensaje("Salida registrada.")
    }
    fun opinar(estrellas: Int,comentario: String) = ejecutar {
        val id = state.value.detalle?.id ?: return@ejecutar
        repository.publicarOpinion(id,estrellas,comentario)
        mutable.update { it.copy(opiniones=repository.opiniones(id)) }; mensaje("Reseña publicada.")
    }
    fun metricas(desde: String,hasta: String) = ejecutar {
        val r = repository.metricas(desde,hasta)
        mutable.update { it.copy(metricas=r) }
    }
}
