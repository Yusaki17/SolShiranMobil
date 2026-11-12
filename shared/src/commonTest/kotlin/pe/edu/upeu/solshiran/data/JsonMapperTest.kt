package pe.edu.upeu.solshiran.data

import kotlinx.serialization.json.Json
import kotlin.test.*
import pe.edu.upeu.solshiran.data.remote.*

class JsonMapperTest {
    @Test fun conservaCapacidadTarifaYFotosDelServidor() {
        val c=Json.parseToJsonElement("""{"idCabana":3,"nombre":"Wasi","aforoMaximo":7,"tarifaNoche":250,"imagenesSecundarias":["/api/v1/cabanas/fotos/abc"],"duenoId":12}""").cabana()
        assertEquals(3L,c.id)
        assertEquals(7,c.capacidad)
        assertEquals(250.0,c.tarifa)
        assertEquals(12L,c.duenoId)
        assertEquals(1,c.fotos.size)
    }
    @Test fun normalizaRolesExistentesSinOtorgarAdministracion() {
        assertEquals("CLIENTE",normalizarRol("USUARIO"))
        assertEquals("DUENO",normalizarRol("DUEÑO"))
        assertEquals("TRABAJADOR",normalizarRol("EMPLEADO"))
        assertEquals("DESCONOCIDO",normalizarRol("desconocido"))
    }
}
