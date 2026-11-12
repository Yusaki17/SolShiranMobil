package pe.edu.upeu.solshiran.data.repository

import app.cash.sqldelight.db.SqlDriver
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.call.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import pe.edu.upeu.solshiran.data.remote.*
import pe.edu.upeu.solshiran.db.SolDb
import pe.edu.upeu.solshiran.domain.model.*
import pe.edu.upeu.solshiran.domain.repository.*
import pe.edu.upeu.solshiran.platform.tokenVigente

class SolRepositoryImpl(private val client: HttpClient, driver: SqlDriver,
    private val almacen: AlmacenSesion, override val urlBase: String = URL_BASE) : SolRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val db = SolDb(driver)
    private val actual = MutableStateFlow(almacen.leer()?.takeIf { tokenVigente(it.token) })
    override val sesion = actual.asStateFlow()
    init { if (actual.value == null) almacen.guardar(null) }

    override fun salir() { almacen.guardar(null); actual.value = null }
    private fun guardar(s: Sesion) { almacen.guardar(s); actual.value = s }
    private fun token(): String {
        val s = actual.value
        if (s == null || !tokenVigente(s.token)) { salir(); throw ErrorApi(401, "Tu sesión venció. Inicia sesión nuevamente.") }
        return s.token
    }
    private suspend fun comprobar(r: HttpResponse) {
        if (r.status.value in 200..299) return
        if (r.status.value == 401) salir()
        val cuerpo = runCatching { json.parseToJsonElement(r.bodyAsText()) }.getOrNull()
        throw ErrorApi(r.status.value, cuerpo?.texto("detail")?.takeIf { it.isNotBlank() }
            ?: cuerpo?.texto("message")?.takeIf { it.isNotBlank() }
            ?: when(r.status.value) { 401 -> "Inicia sesión nuevamente."; 403 -> "No tienes permiso para esta acción."; 409 -> "Los datos entran en conflicto con un registro existente."; else -> "No se pudo completar la operación (${r.status.value})." })
    }
    private suspend fun pedir(ruta: String, metodo: HttpMethod = HttpMethod.Get,
        datos: JsonElement? = null, privado: Boolean = true): JsonElement {
        val credencial = if (privado) token() else null
        val respuesta = client.request("$urlBase/$ruta") {
            header("ngrok-skip-browser-warning", "true")
            method = metodo
            credencial?.let { bearerAuth(it) }
            datos?.let { contentType(ContentType.Application.Json); setBody(it.toString()) }
        }
        comprobar(respuesta)
        val texto = respuesta.bodyAsText()
        return if (texto.isBlank()) JsonNull else json.parseToJsonElement(texto)
    }
    override suspend fun entrar(email: String, password: String) {
        val r = pedir("auth/login", HttpMethod.Post, objeto("email" to email.trim(), "password" to password), false)
        val perfil = Sesion(r.texto("token"), r.id("userId"), r.texto("nombre"), r.texto("email"), normalizarRol(r.texto("rol")))
        require(tokenVigente(perfil.token)) { "El servidor devolvió una sesión inválida." }
        guardar(perfil)
    }
    override suspend fun registrar(nombre: String, email: String, password: String) {
        require(nombre.trim().isNotBlank() && '@' in email && password.length >= 8) { "Completa nombre, correo y una contraseña de al menos 8 caracteres." }
        pedir("usuarios/register", HttpMethod.Post, objeto("nombre" to nombre.trim(), "email" to email.trim(), "password" to password), false)
    }
    override suspend fun catalogo(): List<Cabana> {
        val r = pedir("cabanas", privado = false) as JsonArray
        withContext(Dispatchers.Default) { db.catalogoQueries.guardar(r.toString()) }
        return r.map { it.cabana() }
    }
    override suspend fun catalogoLocal() = withContext(Dispatchers.Default) {
        db.catalogoQueries.leer().executeAsOneOrNull()?.let { json.parseToJsonElement(it).jsonArray.map { x -> x.cabana() } } ?: emptyList()
    }
    override suspend fun detalle(id: Long) = pedir("cabanas/$id", privado = false).cabana()
    override suspend fun fechasOcupadas(idCabana: Long, desde: String, hasta: String): Set<String> {
        val r = pedir("reservas/cabanas/$idCabana/fechas-ocupadas?desde=$desde&hasta=$hasta", privado=false)
        return r.jsonObject["fechasOcupadas"]?.jsonArray?.map { it.jsonPrimitive.content }?.toSet() ?: emptySet()
    }
    override suspend fun cotizar(consulta: Consulta): Cotizacion {
        val r = pedir("reservas/cabanas/${consulta.cabanaId}/disponibilidad?fechaIngreso=${consulta.ingreso}&fechaSalida=${consulta.salida}&cantidadHuespedes=${consulta.huespedes}", privado = false)
        return Cotizacion(consulta, r.bandera("disponible"), r.numero("noches").toInt(), r.numero("montoTotal"), r.numero("montoAdelanto"), r.texto("mensaje"))
    }
    override suspend fun reservar(cotizacion: Cotizacion): Long {
        require(cotizacion.disponible) { "Las fechas no están disponibles." }
        val c = cotizacion.consulta
        return pedir("reservas", HttpMethod.Post, objeto("idCabana" to c.cabanaId, "fechaIngreso" to c.ingreso, "fechaSalida" to c.salida, "cantidadHuespedes" to c.huespedes)).id("idReserva")
    }
    override suspend fun reservas(pagina: Int): List<Reserva> {
        val ruta = if(actual.value?.rol == "CLIENTE") "reservas/mis-reservas?grupo=TODAS" else "reservas/panel?buscar="
        return pedir("$ruta&page=$pagina&size=20").lista("content").map {
            Reserva(it.id("idReserva"), it.texto("nombreCabana", "Cabaña #${it.id("idCabana")}"), it.texto("fechaIngreso"), it.texto("fechaSalida"),
                it.texto("estadoReserva"), it.numero("montoTotal"), it.numero("montoAdelanto"), it.numero("saldoPendiente"), it.bandera("pagoDisponible"))
        }
    }
    override suspend fun pagar(idReserva: Long): Pago {
        val r = pedir("pagos/reservas/$idReserva/orden", HttpMethod.Post, objeto("payerEmail" to actual.value?.email))
        return Pago(r.id("idTransaccion"), r.texto("checkoutUrl"), r.numero("monto"))
    }
    override suspend fun estadoPago(id: Long) = pedir("pagos/transacciones/$id").texto("estado")
    override suspend fun perfil() = pedir("usuarios/me").cuenta()
    override suspend fun guardarPerfil(nombre: String, email: String) {
        val s = actual.value ?: throw ErrorApi(401,"Inicia sesión.")
        val r = pedir("usuarios/${s.id}", HttpMethod.Put, objeto("nombre" to nombre.trim(), "email" to email.trim())).cuenta()
        guardar(s.copy(nombre = r.nombre, email = r.email))
    }
    override suspend fun fotoPerfil(): ByteArray? {
        val r = client.get("$urlBase/usuarios/me/foto") { bearerAuth(token()) }
        if(r.status.value == 404) return null
        comprobar(r)
        return r.body()
    }
    private suspend fun subir(ruta: String, foto: FotoElegida, metodo: HttpMethod): JsonElement {
        require(foto.bytes.size in 1..(2*1024*1024) && foto.tipo in listOf("image/jpeg","image/png")) { "Selecciona una foto JPG o PNG de hasta 2 MB." }
        val r = client.request("$urlBase/$ruta") {
            method = metodo; bearerAuth(token())
            setBody(MultiPartFormDataContent(formData {
                append("archivo", foto.bytes, Headers.build {
                    append(HttpHeaders.ContentType, foto.tipo)
                    append(HttpHeaders.ContentDisposition, "filename=\"foto.${if(foto.tipo == "image/png") "png" else "jpg"}\"")
                })
            }))
        }
        comprobar(r)
        val texto = r.bodyAsText()
        return if(texto.isBlank()) JsonNull else json.parseToJsonElement(texto)
    }
    override suspend fun guardarFotoPerfil(foto: FotoElegida) { subir("usuarios/me/foto",foto,HttpMethod.Put) }
    override suspend fun quitarFotoPerfil() { pedir("usuarios/me/foto",HttpMethod.Delete) }
    override suspend fun imagen(url: String): ByteArray {
        val origen = urlBase.substringBefore("/api/v1")
        val destino = if (url.startsWith("/api/")) origen + url
            else if (url.startsWith("http://localhost:")) origen + url.substringAfter(":18061", "")
            else url
        require(destino.startsWith("http://") || destino.startsWith("https://")) { "Imagen no disponible." }
          val r = client.get(destino) { header("ngrok-skip-browser-warning", "true") }
        comprobar(r)
        return r.body()
    }
    override suspend fun cabanasGestion() = (pedir("cabanas/gestion") as JsonArray).map { it.cabana() }
    override suspend fun guardarCabana(cabana: Cabana) {
        require(cabana.nombre.isNotBlank() && cabana.descripcion.isNotBlank() && cabana.tarifa > 0 && cabana.capacidad > 0) { "Completa los datos de la cabaña." }
        val c = cabana
        pedir("cabanas" + if(c.id > 0) "/${c.id}" else "", if(c.id > 0) HttpMethod.Put else HttpMethod.Post,
            objeto("nombre" to c.nombre, "descripcion" to c.descripcion, "aforoMaximo" to c.capacidad, "numHabitaciones" to c.habitaciones,
                "numBanos" to c.banos, "tarifaNoche" to c.tarifa, "imagenPrincipal" to c.imagen, "imagenesSecundarias" to JsonArray(c.fotos.map(::JsonPrimitive)), "duenoId" to c.duenoId))
    }
    override suspend fun desactivarCabana(id: Long) { pedir("cabanas/$id",HttpMethod.Delete) }
    override suspend fun activarCabana(id: Long) { pedir("cabanas/$id/activar",HttpMethod.Put,objeto()) }
    override suspend fun subirFoto(foto: FotoElegida): String = subir("cabanas/fotos",foto,HttpMethod.Post).texto("url")
    override suspend fun cuentas() = (pedir("usuarios") as JsonArray).map { it.cuenta() }
    override suspend fun guardarCuenta(cuenta: Cuenta, password: String) {
        val c = cuenta
        pedir("usuarios" + if(c.id > 0) "/${c.id}" else "", if(c.id > 0) HttpMethod.Put else HttpMethod.Post,
            objeto("nombre" to c.nombre, "email" to c.email, "rol" to c.rol, "duenoId" to c.duenoId, "password" to password))
    }
    override suspend fun eliminarCuenta(id: Long) { pedir("usuarios/$id",HttpMethod.Delete) }
    override suspend fun checkin(id: Long) { pedir("reservas/$id/check-in",HttpMethod.Post,objeto()) }
    override suspend fun cobrarSaldo(id: Long, medio: String, operacion: String, clave: String) {
        require(medio == "EFECTIVO" || operacion.isNotBlank()) { "Ingresa el número de operación." }
        pedir("pagos/reservas/$id/restante",HttpMethod.Post,objeto("metodoPago" to medio,
            "numeroOperacion" to operacion.takeIf {it.isNotBlank()},"claveIdempotencia" to clave))
    }
    override suspend fun checkout(id: Long, descripcion: String, monto: Double, medio: String) {
        val datos = if(descripcion.isBlank()) objeto() else objeto("danos" to objeto("descripcion" to descripcion,"monto" to monto,"medioPago" to medio))
        pedir("reservas/$id/check-out",HttpMethod.Post,datos)
    }
    override suspend fun opiniones(idCabana: Long) = pedir("resenas/cabanas/$idCabana?page=0&size=20",privado=false).lista("opiniones").map {
        Opinion(it.texto("autor"),it.numero("puntuacion").toInt(),it.texto("comentario"))
    }
    override suspend fun publicarOpinion(idCabana: Long, puntuacion: Int, comentario: String) {
        require(puntuacion in 1..5 && comentario.trim().isNotBlank()) { "Selecciona estrellas y escribe un comentario." }
        val id = pedir("resenas/cabanas/$idCabana/elegibilidad").id("idReserva")
        require(id > 0) { "Necesitas una estancia finalizada sin reseña para calificar." }
        pedir("resenas/cabanas/$idCabana",HttpMethod.Post,objeto("idReserva" to id,"puntuacion" to puntuacion,"comentario" to comentario.trim()))
    }
    override suspend fun metricas(desde: String, hasta: String): Metricas {
        require(hasta >= desde) { "Revisa el período seleccionado." }
        val ingreso = pedir("pagos/metricas/ingresos?desde=$desde&hasta=$hasta")
        val reservas = pedir("reservas/metricas/frecuencia?desde=$desde&hasta=$hasta")
        return Metricas(ingreso.numero("totalIngresos"),ingreso.numero("numeroPagos").toInt(),reservas.numero("totalReservas").toInt(),
            reservas.lista("ranking").map { it.id("idCabana") to it.numero("reservas").toInt() })
    }
}
