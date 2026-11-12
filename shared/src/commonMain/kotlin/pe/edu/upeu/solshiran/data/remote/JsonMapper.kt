package pe.edu.upeu.solshiran.data.remote

import kotlinx.serialization.json.*
import pe.edu.upeu.solshiran.domain.model.*

internal fun JsonElement.texto(clave: String, defecto: String = "") =
    (this as? JsonObject)?.get(clave)?.let { (it as? JsonPrimitive)?.contentOrNull } ?: defecto
internal fun JsonElement.numero(clave: String) = texto(clave).toDoubleOrNull() ?: 0.0
internal fun JsonElement.id(clave: String) = texto(clave).toLongOrNull() ?: 0L
internal fun JsonElement.bandera(clave: String, defecto: Boolean = false) = texto(clave).toBooleanStrictOrNull() ?: defecto
internal fun JsonElement.lista(clave: String) = (this as? JsonObject)?.get(clave) as? JsonArray ?: JsonArray(emptyList())
internal fun JsonElement.cabana() = Cabana(id("idCabana"), texto("nombre"), texto("descripcion"),
    numero("aforoMaximo").toInt(), numero("numHabitaciones").toInt(), numero("numBanos").toInt(),
    numero("tarifaNoche"), texto("imagenPrincipal"), lista("imagenesSecundarias").mapNotNull { it.jsonPrimitive.contentOrNull },
    bandera("activo", !texto("estado").equals("INACTIVA", true)), texto("duenoId").toLongOrNull())
internal fun JsonElement.cuenta() = Cuenta(id("id"), texto("nombre"), texto("email"), normalizarRol(texto("rol")),
    bandera("activo", true), texto("duenoId").toLongOrNull())
fun normalizarRol(rol: String) = when (rol.uppercase()) {
    "USUARIO" -> "CLIENTE"
    "DUEÑO", "PROPIETARIO" -> "DUENO"
    "EMPLEADO", "GUARDIAN" -> "TRABAJADOR"
    else -> rol.uppercase()
}
internal fun objeto(vararg valores: Pair<String, Any?>): JsonObject = buildJsonObject {
    valores.forEach { (clave, valor) -> put(clave, when(valor) {
        null -> JsonNull
        is JsonElement -> valor
        is Boolean -> JsonPrimitive(valor)
        is Number -> JsonPrimitive(valor)
        else -> JsonPrimitive(valor.toString())
    }) }
}
