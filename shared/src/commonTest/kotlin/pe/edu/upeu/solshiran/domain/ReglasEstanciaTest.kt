package pe.edu.upeu.solshiran.domain

import kotlin.test.*
import pe.edu.upeu.solshiran.domain.usecase.*

class ReglasEstanciaTest {
    @Test fun bloqueaUnaNocheOcupadaDentroDelRango() {
        assertFalse(rangoLibre("2026-11-13","2026-11-17",setOf("2026-11-15")))
        assertFalse(rangoLibre("2026-11-15","2026-11-17",setOf("2026-11-15")))
    }
    @Test fun permiteSalirElDiaQueEmpiezaOtraReserva() {
        assertTrue(rangoLibre("2026-11-13","2026-11-15",setOf("2026-11-15")))
    }
    @Test fun noCobraAntesDelIngresoNiDesdeLaSalida() {
        assertFalse(puedeIniciarEstancia("CONFIRMADA","2026-11-14","2026-11-16","2026-11-13"))
        assertTrue(puedeIniciarEstancia("CONFIRMADA","2026-11-14","2026-11-16","2026-11-14"))
        assertTrue(puedeIniciarEstancia("CONFIRMADA","2026-11-14","2026-11-16","2026-11-15"))
        assertFalse(puedeIniciarEstancia("CONFIRMADA","2026-11-14","2026-11-16","2026-11-16"))
        assertFalse(puedeIniciarEstancia("PENDIENTE_PAGO","2026-11-14","2026-11-16","2026-11-14"))
    }
}
