package com.example.data

object CubaGeographyData {

    val CUBA_BOUNDS_MIN_LAT = 19.80
    val CUBA_BOUNDS_MAX_LAT = 23.30
    val CUBA_BOUNDS_MIN_LON = -85.00
    val CUBA_BOUNDS_MAX_LON = -74.10

    // Center of Cuba map (Geographic center near Sancti Spíritus / Ciego)
    val CUBA_CENTER = GeoPoint(21.90, -79.50, "Cuba")
    val HAVANA_CENTER = GeoPoint(23.1136, -82.3666, "La Habana")

    val PROVINCES = listOf(
        "Pinar del Río", "Artemisa", "La Habana", "Mayabeque", "Matanzas",
        "Cienfuegos", "Villa Clara", "Sancti Spíritus", "Ciego de Ávila",
        "Camagüey", "Las Tunas", "Holguín", "Granma", "Santiago de Cuba",
        "Guantánamo", "Isla de la Juventud"
    )

    val CITIES = listOf(
        GeoPoint(23.1136, -82.3666, "La Habana"),
        GeoPoint(20.0208, -75.8267, "Santiago de Cuba"),
        GeoPoint(20.8872, -76.2631, "Holguín"),
        GeoPoint(21.3808, -77.9169, "Camagüey"),
        GeoPoint(22.4069, -79.9647, "Santa Clara"),
        GeoPoint(23.0411, -81.5775, "Matanzas"),
        GeoPoint(23.1568, -81.2444, "Varadero"),
        GeoPoint(22.1496, -80.4466, "Cienfuegos"),
        GeoPoint(21.9333, -79.4444, "Sancti Spíritus"),
        GeoPoint(22.4167, -83.7000, "Pinar del Río"),
        GeoPoint(21.7815, -79.9842, "Trinidad"),
        GeoPoint(21.8400, -78.7619, "Ciego de Ávila"),
        GeoPoint(20.9597, -76.9544, "Las Tunas"),
        GeoPoint(20.3792, -76.6433, "Bayamo"),
        GeoPoint(20.1444, -75.2092, "Guantánamo"),
        GeoPoint(20.3475, -74.4961, "Baracoa"),
        GeoPoint(22.6156, -83.7083, "Viñales"),
        GeoPoint(22.8139, -82.7561, "Artemisa"),
        GeoPoint(22.9614, -82.1511, "San José de las Lajas"),
        GeoPoint(21.8800, -82.8000, "Nueva Gerona")
    )

    // Extensive Offline POI Database for Cuba
    val POI_DATABASE = listOf(
        // Servicentros CUPET / Oro Negro
        PointOfInterest("cupet_01", "Servicentro CUPET Zapata y 4", PoiCategory.GASOLINERA, "La Habana", 23.1285, -82.3950, "Calle Zapata esq. Calle 4, Vedado", "+53 7 830-1234", "Abierto 24h. Gasolina Especial, Regular y Diésel"),
        PointOfInterest("cupet_02", "Servicentro CUPET Riviera", PoiCategory.GASOLINERA, "La Habana", 23.1388, -82.4095, "Malecón y Paseo, Vedado", "+53 7 833-4567", "CUPET frente al Malecón con tienda"),
        PointOfInterest("cupet_03", "Servicentro CUPET 5ta y 112", PoiCategory.GASOLINERA, "La Habana", 23.0935, -82.4490, "5ta Avenida y Calle 112, Playa", "+53 7 204-7890", "Servicentro de alta concurrencia"),
        PointOfInterest("cupet_04", "Servicentro Oro Negro El Rápido", PoiCategory.GASOLINERA, "Matanzas", 23.0450, -81.5680, "Vía Blanca km 101, Matanzas", "+53 45 24-5678", "Ubicado en autopista Vía Blanca"),
        PointOfInterest("cupet_05", "Servicentro CUPET Varadero Autopista", PoiCategory.GASOLINERA, "Matanzas", 23.1350, -81.2890, "Autopista Sur km 12, Varadero", "+53 45 61-2233", "Servicentro principal península de Hicacos"),
        PointOfInterest("cupet_06", "Servicentro CUPET Ocho Vías km 259", PoiCategory.GASOLINERA, "Villa Clara", 22.3900, -80.0100, "Autopista Nacional km 259, Santa Clara", "+53 42 28-9900", "Parador y combustible en Autopista"),
        PointOfInterest("cupet_07", "Servicentro CUPET Plaza de la Revolución", PoiCategory.GASOLINERA, "Santiago de Cuba", 20.0310, -75.8150, "Av. de las Américas, Santiago", "+53 22 64-1122", "Combustible y ponchera 24h"),
        PointOfInterest("cupet_08", "Servicentro CUPET Los Álamos", PoiCategory.GASOLINERA, "Holguín", 20.8950, -76.2550, "Carretera Central km 775, Holguín", "+53 24 46-7788", "Servicio integral y lubricantes"),
        PointOfInterest("cupet_09", "Servicentro CUPET La Caridad", PoiCategory.GASOLINERA, "Camagüey", 21.3850, -77.9100, "Av. de la Libertad, Camagüey", "+53 32 29-4455", "Combustible y minimercado"),
        PointOfInterest("cupet_10", "Servicentro CUPET Autopista km 140", PoiCategory.GASOLINERA, "Matanzas", 22.7500, -81.1000, "Autopista Nacional km 140, Jagüey Grande", "+53 45 91-1122", "Parador Jagüey Grande"),

        // Hospitales & Urgencias
        PointOfInterest("hosp_01", "Hospital Clínico Quirúrgico Hermanos Ameijeiras", PoiCategory.HOSPITAL, "La Habana", 23.1415, -82.3780, "San Lázaro No. 701, Centro Habana", "+53 7 876-1000", "Hospital de referencia nacional y urgencias"),
        PointOfInterest("hosp_02", "Hospital Universitario General Calixto García", PoiCategory.HOSPITAL, "La Habana", 23.1362, -82.3845, "Av. Universidad y J, Vedado", "+53 7 838-2197", "Servicio de urgencias 24h"),
        PointOfInterest("hosp_03", "Hospital Ortopédico Frank País", PoiCategory.HOSPITAL, "La Habana", 23.0780, -82.4410, "Av. 51 No. 19603, La Lisa", "+53 7 260-2000", "Centro ortopédico internacional"),
        PointOfInterest("hosp_04", "Hospital Provincial Arnaldo Milián Castro", PoiCategory.HOSPITAL, "Villa Clara", 22.4210, -79.9540, "Circunvalación Norte, Santa Clara", "+53 42 27-1500", "Hospital provincial de Villa Clara"),
        PointOfInterest("hosp_05", "Hospital Provincial Saturnino Lora", PoiCategory.HOSPITAL, "Santiago de Cuba", 20.0240, -75.8200, "Av. de los Libertadores, Santiago", "+53 22 64-3011", "Urgencias y especialidades"),
        PointOfInterest("hosp_06", "Hospital Vladimir Ilich Lenin", PoiCategory.HOSPITAL, "Holguín", 20.8820, -76.2690, "Av. Lenin No. 4, Holguín", "+53 24 42-5302", "Centro hospitalario provincial"),
        PointOfInterest("hosp_07", "Hospital General Dr. Gustavo Aldereguía Lima", PoiCategory.HOSPITAL, "Cienfuegos", 22.1380, -80.4320, "Calle 51A y Av. 5 de Septiembre", "+53 43 51-5001", "Hospital general de Cienfuegos"),
        PointOfInterest("hosp_08", "Hospital Faustino Pérez", PoiCategory.HOSPITAL, "Matanzas", 23.0330, -81.5850, "Carretera Central km 101, Matanzas", "+53 45 24-7014", "Atención médica integral"),

        // Talleres Mecánicos y Poncheras
        PointOfInterest("tal_01", "Taller Mecánico y Ponchera Vedado", PoiCategory.TALLER_PONCHERA, "La Habana", 23.1310, -82.3980, "Calle Línea y 12, Vedado", "+53 5 280-4433", "Reparación de neumáticos, balanceo y mecánica ligera"),
        PointOfInterest("tal_02", "Ponchera Autopista Nacional km 90", PoiCategory.TALLER_PONCHERA, "Mayabeque", 22.8800, -81.9200, "Autopista Nacional km 90, Güines", "+53 5 330-8811", "Ponchera 24h y auxilio vial"),
        PointOfInterest("tal_03", "Taller Automotriz SASA Santa Clara", PoiCategory.TALLER_PONCHERA, "Villa Clara", 22.4020, -79.9700, "Carretera a Camajuaní km 2", "+53 42 20-3344", "Taller especializado y repuestos"),
        PointOfInterest("tal_04", "Ponchera y Electricidad Automotriz Santiago", PoiCategory.TALLER_PONCHERA, "Santiago de Cuba", 20.0180, -75.8320, "Carretera del Morro km 1", "+53 5 450-9922", "Vulcanizado y servicio eléctrico"),

        // Terminales y Transporte
        PointOfInterest("trans_01", "Terminal de Ómnibus Nacionales Viazul / Astro", PoiCategory.TRANSPORTE, "La Habana", 23.1235, -82.3830, "Av. Rancho Boyeros y 19 de Mayo", "+53 7 881-1413", "Salidas nacionales interprovinciales"),
        PointOfInterest("trans_02", "Estación Central de Ferrocarriles", PoiCategory.TRANSPORTE, "La Habana", 23.1290, -82.3560, "Avenida de Bélgica esq. Arsenal, Habana Vieja", "+53 7 862-3151", "Terminal central de trenes de Cuba"),
        PointOfInterest("trans_03", "Aeropuerto Internacional José Martí (HAV)", PoiCategory.TRANSPORTE, "La Habana", 22.9892, -82.4092, "Av. Van Troy, Boyeros, La Habana", "+53 7 266-4133", "Terminales 2 y 3 Vuelos Internacionales"),
        PointOfInterest("trans_04", "Aeropuerto Internacional Juan Gualberto Gómez (VRA)", PoiCategory.TRANSPORTE, "Matanzas", 23.0339, -81.4358, "Carretera a Varadero km 5", "+53 45 61-3016", "Aeropuerto de Varadero"),
        PointOfInterest("trans_05", "Terminal de Ómnibus Viazul Santiago", PoiCategory.TRANSPORTE, "Santiago de Cuba", 20.0290, -75.8240, "Carretera Central y Libertadores", "+53 22 62-8484", "Llegadas y salidas Viazul"),

        // Turismo, Hoteles y Patrimonio
        PointOfInterest("tur_01", "Capitolio Nacional de Cuba", PoiCategory.TURISMO, "La Habana", 23.1353, -82.3592, "Paseo del Prado No. 513, Habana Vieja", "+53 7 860-3411", "Km 0 de la Carretera Central de Cuba"),
        PointOfInterest("tur_02", "Castillo de los Tres Reyes del Morro", PoiCategory.TURISMO, "La Habana", 23.1506, -82.3570, "Parque Histórico Militar Morro-Cabaña", "+53 7 863-7941", "Fortaleza colonial y faro de La Habana"),
        PointOfInterest("tur_03", "Hotel Nacional de Cuba", PoiCategory.HOTEL_CAMPISMO, "La Habana", 23.1420, -82.3815, "Calle O esq. 21, Vedado", "+53 7 836-3564", "Monumento Nacional y hotel emblemático"),
        PointOfInterest("tur_04", "Plaza Mayor y Centro Histórico de Trinidad", PoiCategory.TURISMO, "Sancti Spíritus", 21.8055, -79.9840, "Calle Real No. 45, Trinidad", "+53 41 99-3120", "Patrimonio de la Humanidad UNESCO"),
        PointOfInterest("tur_05", "Valle de Viñales y Mural de la Prehistoria", PoiCategory.TURISMO, "Pinar del Río", 22.6180, -83.7140, "Valle de Dos Hermanas, Viñales", "+53 48 79-6260", "Paisaje cultural de la humanidad"),
        PointOfInterest("tur_06", "Santuario de Nuestra Señora de la Caridad del Cobre", PoiCategory.TURISMO, "Santiago de Cuba", 20.0489, -75.9503, "El Cobre, Santiago de Cuba", "+53 22 34-6103", "Patrona de Cuba y sitio de peregrinación"),
        PointOfInterest("tur_07", "Conjunto Escultórico Memorial Che Guevara", PoiCategory.TURISMO, "Villa Clara", 22.4025, -79.9792, "Plaza de la Revolución, Santa Clara", "+53 42 20-5879", "Monumento y mausoleo histórico"),
        PointOfInterest("tur_08", "Faro de Baracoa y La Farola", PoiCategory.TURISMO, "Guantánamo", 20.3520, -74.4920, "Punta de Maisí / Baracoa", "+53 21 64-2110", "Primera villa de Cuba y viaducto La Farola"),

        // Farmacias y Bancos
        PointOfInterest("farm_01", "Farmacia Principal Taquechel", PoiCategory.FARMACIA, "La Habana", 23.1385, -82.3530, "Calle Obispo No. 155, Habana Vieja", "+53 7 862-9286", "Farmacia museo y dispensario"),
        PointOfInterest("farm_02", "Farmacia Especial de Turno Vedado", PoiCategory.FARMACIA, "La Habana", 23.1370, -82.3920, "Calle 23 y L, Vedado", "+53 7 832-5566", "Medicamentos de turno 24 horas"),
        PointOfInterest("banc_01", "Banco Metropolitano y Cajeros RED", PoiCategory.BANCO_CAJERO, "La Habana", 23.1360, -82.3910, "Calle 23 y Montero Sánchez, Vedado", "+53 7 830-9911", "Cajeros automáticos y cambio Cadeca")
    )

    // Major Highways and Interprovincial Trunk Roads with Realistic Geometry
    val TRUNK_ROADS = listOf(
        RoadSegment(
            id = "autopista_nacional_a1",
            name = "Autopista Nacional (A1 / Ocho Vías)",
            highwayType = "motorway",
            speedLimit = 100,
            points = listOf(
                GeoPoint(23.0300, -82.3200, "Enlace Habana (Primer Anillo)"),
                GeoPoint(22.9500, -82.1600, "San José de las Lajas"),
                GeoPoint(22.8800, -81.9200, "Güines"),
                GeoPoint(22.7500, -81.1000, "Jagüey Grande (km 140)"),
                GeoPoint(22.6200, -80.6500, "Aguada de Pasajeros (km 172)"),
                GeoPoint(22.4500, -80.2000, "Ranchuelo"),
                GeoPoint(22.3900, -79.9800, "Santa Clara (km 260)"),
                GeoPoint(22.1900, -79.5200, "Cabaiguán"),
                GeoPoint(21.9300, -79.4400, "Sancti Spíritus"),
                GeoPoint(21.8400, -78.7600, "Ciego de Ávila"),
                GeoPoint(21.5500, -78.2000, "Florida"),
                GeoPoint(21.3800, -77.9100, "Camagüey"),
                GeoPoint(20.9500, -76.9500, "Las Tunas"),
                GeoPoint(20.8800, -76.2600, "Holguín"),
                GeoPoint(20.3700, -76.6400, "Bayamo"),
                GeoPoint(20.1800, -76.0500, "Contramaestre"),
                GeoPoint(20.0200, -75.8200, "Santiago de Cuba")
            )
        ),
        RoadSegment(
            id = "carretera_central_ruta_1",
            name = "Carretera Central (Ruta 1)",
            highwayType = "primary",
            speedLimit = 80,
            points = listOf(
                GeoPoint(22.4167, -83.7000, "Pinar del Río"),
                GeoPoint(22.5800, -83.3500, "Consolación del Sur"),
                GeoPoint(22.7200, -83.0500, "San Cristóbal"),
                GeoPoint(22.8100, -82.7500, "Artemisa"),
                GeoPoint(23.0000, -82.4800, "Bauta"),
                GeoPoint(23.1136, -82.3666, "La Habana (Capitolio Km 0)"),
                GeoPoint(23.0500, -82.1500, "San José de las Lajas"),
                GeoPoint(23.0200, -81.8500, "Madruga"),
                GeoPoint(23.0411, -81.5775, "Matanzas"),
                GeoPoint(22.9500, -81.1800, "Jovellanos"),
                GeoPoint(22.7100, -80.9000, "Colón"),
                GeoPoint(22.5800, -80.4500, "Santo Domingo"),
                GeoPoint(22.4069, -79.9647, "Santa Clara"),
                GeoPoint(22.3100, -79.6500, "Placetas"),
                GeoPoint(22.1900, -79.4900, "Cabaiguán"),
                GeoPoint(21.9333, -79.4444, "Sancti Spíritus"),
                GeoPoint(21.8900, -79.2000, "Jatibonico"),
                GeoPoint(21.8400, -78.7619, "Ciego de Ávila"),
                GeoPoint(21.5200, -78.2200, "Florida"),
                GeoPoint(21.3808, -77.9169, "Camagüey"),
                GeoPoint(21.2100, -77.4500, "Guáimaro"),
                GeoPoint(20.9597, -76.9544, "Las Tunas"),
                GeoPoint(20.8872, -76.2631, "Holguín"),
                GeoPoint(20.5500, -76.4500, "Cacocum"),
                GeoPoint(20.3792, -76.6433, "Bayamo"),
                GeoPoint(20.3200, -76.2500, "Jiguaní"),
                GeoPoint(20.1400, -75.9900, "Palma Soriano"),
                GeoPoint(20.0208, -75.8267, "Santiago de Cuba"),
                GeoPoint(20.1444, -75.2092, "Guantánamo"),
                GeoPoint(20.3475, -74.4961, "Baracoa (Viaducto La Farola)")
            )
        ),
        RoadSegment(
            id = "via_blanca",
            name = "Vía Blanca (Habana - Matanzas - Varadero)",
            highwayType = "primary",
            speedLimit = 90,
            points = listOf(
                GeoPoint(23.1400, -82.3500, "Túnel de la Bahía de La Habana"),
                GeoPoint(23.1650, -82.2500, "Habana del Este / Cojímar"),
                GeoPoint(23.1700, -82.1500, "Playas del Este (Guanabo)"),
                GeoPoint(23.1550, -81.9200, "Santa Cruz del Norte (Puente Bacunayagua)"),
                GeoPoint(23.0500, -81.6500, "Mirador Bacunayagua"),
                GeoPoint(23.0411, -81.5775, "Matanzas (Bahía)"),
                GeoPoint(23.0800, -81.4200, "Boca de Camarioca"),
                GeoPoint(23.1350, -81.2890, "Puente de Varadero (Paso Malo)"),
                GeoPoint(23.1568, -81.2444, "Varadero (Península de Hicacos)")
            )
        ),
        RoadSegment(
            id = "circuito_sur_cienfuegos_trinidad",
            name = "Circuito Sur (Cienfuegos - Trinidad - Sancti Spíritus)",
            highwayType = "secondary",
            speedLimit = 70,
            points = listOf(
                GeoPoint(22.1496, -80.4466, "Cienfuegos"),
                GeoPoint(21.9800, -80.2500, "San Juan de los Yeras"),
                GeoPoint(21.8055, -79.9840, "Trinidad"),
                GeoPoint(21.8400, -79.6800, "Valle de los Ingenios"),
                GeoPoint(21.9333, -79.4444, "Sancti Spíritus")
            )
        ),
        RoadSegment(
            id = "autopista_nacional_pinar",
            name = "Autopista Este-Oeste (Habana - Pinar del Río)",
            highwayType = "motorway",
            speedLimit = 100,
            points = listOf(
                GeoPoint(23.0800, -82.4600, "La Habana (La Lisa / 114)"),
                GeoPoint(22.9900, -82.6000, "Bauta / Caimito"),
                GeoPoint(22.8400, -82.8000, "Guanajay / Artemisa"),
                GeoPoint(22.7500, -83.0000, "Candelaria"),
                GeoPoint(22.7000, -83.1500, "San Cristóbal"),
                GeoPoint(22.5600, -83.4000, "Los Palacios"),
                GeoPoint(22.4800, -83.5500, "Consolación del Sur"),
                GeoPoint(22.4167, -83.7000, "Pinar del Río")
            )
        ),
        RoadSegment(
            id = "habana_arteries",
            name = "Avenida Malecón & 23 Vedado",
            highwayType = "primary",
            speedLimit = 50,
            points = listOf(
                GeoPoint(23.1480, -82.3540, "Castillo de la Real Fuerza"),
                GeoPoint(23.1440, -82.3650, "Malecón y Prado"),
                GeoPoint(23.1415, -82.3780, "Malecón y Belascoaín"),
                GeoPoint(23.1388, -82.4095, "Malecón y Calle G / Paseo"),
                GeoPoint(23.1320, -82.4180, "Túnel de 5ta Avenida"),
                GeoPoint(23.1100, -82.4400, "5ta Avenida y Calle 42 (Miramar)")
            )
        )
    )

    // Preloaded offline map packages list
    val OFFLINE_PACKAGES = listOf(
        OfflineMapPackage(
            id = "cuba_full_pbf",
            name = "Cuba Completa (OSM Vector PBF)",
            fileName = "cuba-latest.osm.pbf",
            sizeMb = 98.4,
            versionDate = "Agosto 2026",
            isDownloaded = true,
            isLoadedInEngine = true,
            format = ".osm.pbf",
            description = "Datos completos de carreteras, calles, POIs y costas de Cuba extraídos de OpenStreetMap."
        ),
        OfflineMapPackage(
            id = "cuba_mbtiles_highres",
            name = "Cuba Raster & Vector MBTiles",
            fileName = "cuba_hybrid_z14.mbtiles",
            sizeMb = 145.2,
            versionDate = "Julio 2026",
            isDownloaded = true,
            isLoadedInEngine = true,
            format = ".mbtiles",
            description = "Teselas offline optimizadas para renderizado suave en zoom 6 al 15 sin conexión a internet."
        ),
        OfflineMapPackage(
            id = "habana_detailed_mapsforge",
            name = "La Habana & Occidente Alta Definición",
            fileName = "occidente_cuba_poi.map",
            sizeMb = 42.0,
            versionDate = "Agosto 2026",
            isDownloaded = true,
            isLoadedInEngine = true,
            format = ".map (Mapsforge)",
            description = "Nivel de detalle edificio por edificio, numeración postal y servicentros CUPET de La Habana, Matanzas y Pinar."
        ),
        OfflineMapPackage(
            id = "oriente_cuba_graph",
            name = "Oriente & Santiago Red de Rutas",
            fileName = "oriente_graphhopper_routing.ghz",
            sizeMb = 36.8,
            versionDate = "Agosto 2026",
            isDownloaded = true,
            isLoadedInEngine = true,
            format = ".ghz (GraphHopper)",
            description = "Grafo de enrutamiento optimizado para cálculo instantáneo de rutas y velocidades en provincias orientales."
        )
    )
}
