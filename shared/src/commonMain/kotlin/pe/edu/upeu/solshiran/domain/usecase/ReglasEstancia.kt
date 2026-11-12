package pe.edu.upeu.solshiran.domain.usecase

// Fechas ISO: el día de salida no es una noche ocupada.
fun rangoLibre(entrada: String, salida: String, ocupadas: Set<String>): Boolean =
    entrada.isNotBlank() && salida > entrada && ocupadas.none { it >= entrada && it < salida }

fun puedeIniciarEstancia(estado: String, entrada: String, salida: String, hoy: String): Boolean =
    estado == "CONFIRMADA" && entrada <= hoy && hoy < salida
