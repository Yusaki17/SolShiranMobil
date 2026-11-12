package pe.edu.upeu.solshiran.domain.usecase

import pe.edu.upeu.solshiran.domain.model.Consulta

class ValidarReserva {
    operator fun invoke(consulta: Consulta, hoy: String, capacidad: Int) {
        require(consulta.ingreso.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) { "Selecciona la fecha de ingreso." }
        require(consulta.salida.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) { "Selecciona la fecha de salida." }
        require(consulta.ingreso >= hoy) { "El ingreso no puede estar en el pasado." }
        require(consulta.salida > consulta.ingreso) { "La salida debe ser posterior al ingreso." }
        require(consulta.huespedes in 1..capacidad) { "La cantidad de huéspedes supera la capacidad." }
    }
}
