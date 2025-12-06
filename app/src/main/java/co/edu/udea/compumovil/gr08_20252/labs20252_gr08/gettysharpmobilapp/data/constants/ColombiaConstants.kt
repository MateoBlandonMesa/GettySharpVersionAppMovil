package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.data.constants

object ColombiaConstants {
    val departments = listOf(
        Department("Antioquia", listOf(
            "Medellín", "Envigado", "Bello", "Itagüí", "Rionegro",
            "Apartadó", "Turbo", "La Ceja", "Copacabana", "Sabaneta"
        )),
        Department("Cundinamarca", listOf(
            "Bogotá", "Soacha", "Chía", "Zipaquirá", "Girardot",
            "Cajicá", "Facatativá", "Funza", "Mosquera", "La Mesa"
        )),
        Department("Valle del Cauca", listOf(
            "Cali", "Palmira", "Buenaventura", "Buga", "Tuluá",
            "Jamundí", "Cartago", "Yumbo", "Florida", "Candelaria"
        )),
        Department("Atlántico", listOf(
            "Barranquilla", "Soledad", "Malambo", "Galapa",
            "Puerto Colombia", "Sabanagrande", "Palmar de Varela", "Santo Tomás"
        )),
        Department("Santander", listOf(
            "Bucaramanga", "Floridablanca", "Girón", "Piedecuesta",
            "Barrancabermeja", "San Gil", "Socorro", "Lebrija"
        ))
    )

    val streetTypes = listOf(
        "Calle",
        "Carrera",
        "Avenida",
        "Avenida Calle",
        "Avenida Carrera",
        "Transversal",
        "Diagonal",
        "Circular"
    )

    val streetQualifiers = listOf(
        "Bis",
        "Sur",
        "Este",
        "Oeste"
    )

    val quadrants = listOf(
        "Norte",
        "Sur",
        "Este",
        "Oeste",
        "Noroccidente",
        "Nororiente",
        "Suroccidente",
        "Suroriente"
    )

    val idTypes = listOf(
        IdType("cedula", "Cédula de Ciudadanía"),
        IdType("tarjeta_identidad", "Tarjeta de Identidad"),
        IdType("pasaporte", "Pasaporte"),
        IdType("otro", "Otro")
    )

    data class Department(val name: String, val cities: List<String>)
    data class IdType(val value: String, val label: String)
}

