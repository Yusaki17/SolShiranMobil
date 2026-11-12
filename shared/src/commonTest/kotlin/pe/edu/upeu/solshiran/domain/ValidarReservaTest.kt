package pe.edu.upeu.solshiran.domain

import kotlin.test.*
import pe.edu.upeu.solshiran.domain.model.Consulta
import pe.edu.upeu.solshiran.domain.usecase.ValidarReserva

class ValidarReservaTest {
    private val validar=ValidarReserva()
    @Test fun aceptaRangoValido() { validar(Consulta(1,"2026-12-01","2026-12-03",4),"2026-11-12",4) }
    @Test fun rechazaSalidaAnterior() {
        assertFailsWith<IllegalArgumentException> {validar(Consulta(1,"2026-12-03","2026-12-01",2),"2026-11-12",4)}
    }
    @Test fun rechazaFechaPasada() {
        assertFailsWith<IllegalArgumentException> {validar(Consulta(1,"2026-11-01","2026-12-01",2),"2026-11-12",4)}
    }
    @Test fun rechazaExcesoDeHuespedes() {
        assertFailsWith<IllegalArgumentException> {validar(Consulta(1,"2026-12-01","2026-12-03",5),"2026-11-12",4)}
    }
}
