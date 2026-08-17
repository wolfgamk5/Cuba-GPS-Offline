# Cómo generar el mapa 3D real de Cuba (offline)

Este chat no tiene acceso a internet ni a Android Studio, así que no puedo descargar los
datos ni compilar la APK por ti. Ya dejé el código Kotlin listo (carpeta `app/src/main/java/com/example/maps/`)
para que consuma esos datos en cuanto los generes. Estos son los pasos que **tú** debes hacer,
una sola vez, en tu computadora:

## 1. Descargar el mapa de Cuba (datos reales de calles, edificios, POIs)

OpenStreetMap tiene el mapa completo de Cuba, mantenido por la comunidad y gratis:

- Ve a: https://download.geofabrik.de/central-america/cuba.html
- Descarga `cuba-latest.osm.pbf` (~40-80 MB aprox.)

Esto ya incluye: todas las calles y carreteras, todos los edificios (casas incluidas),
hospitales, policías, farmacias, escuelas, gasolineras, etc. — todo lo que la gente ha
mapeado en Cuba, sin que tengas que escribirlo tú a mano.

## 2. Convertir esos datos a "vector tiles" (formato que el mapa puede dibujar)

Usa **Planetiler** (una herramienta gratis, un solo .jar, no requiere instalar nada más
que Java):

```bash
# Requiere tener Java 17+ instalado
curl -LO https://github.com/onthegomap/planetiler/releases/latest/download/planetiler.jar
java -Xmx4g -jar planetiler.jar --download --area=cuba \
     --output=cuba.mbtiles
```

Esto genera un archivo `cuba.mbtiles` con calles, edificios (para el 3D) y puntos de
interés, ya listo para el teléfono. Puede pesar entre 150 MB y 400 MB dependiendo del
detalle — normal para un país completo.

## 3. Copiar el archivo al teléfono

El código que dejé espera el archivo aquí:

```
/Android/data/com.aistudio.cubagps.nvgtq/files/maps/cuba.mbtiles
```

(en Kotlin: `context.getExternalFilesDir(null)/maps/cuba.mbtiles`). Cópialo por USB o,
mejor, haz que la app lo descargue una vez desde tu propio servidor/Google Drive la
primera vez que haya wifi — así el usuario final no tiene que enchufar el teléfono a
una computadora.

## 4. Qué hace el código que ya integré

- **`MbtilesTileServer.kt`**: levanta un mini servidor HTTP en el propio teléfono
  (`127.0.0.1:8085`) que lee ese `.mbtiles` y sirve los tiles al mapa. Así MapLibre
  cree que está hablando con un servidor de internet, pero todo ocurre offline, en el
  dispositivo.
- **`cuba_style.json`**: define cómo se pinta el mapa — agua, vegetación, carreteras por
  jerarquía (autopista/primaria/secundaria) y la capa **`buildings-3d`**, que extruye
  cada edificio con su altura real (eso es lo que da el efecto 3D de las casas/edificios).
- **`CubaMapLibreView.kt`**: el composable con 3 modos de cámara:
  - `NORMAL_2D` — vista de mapa clásica desde arriba.
  - `FREE_3D` — vista inclinada (55°) para explorar el mapa en 3D.
  - `NAVIGATION_FPV` — cámara en primera persona (65° de inclinación, zoom cercano,
    girando según tu rumbo GPS) para cuando la navegación está activa — como Waze/Google Maps.

## 5. Fase 2 — Rutas reales calle por calle (GraphHopper)

Ya integré el código (`GraphHopperRoutingEngine.kt`) y lo conecté en la app: si encuentra
el grafo de rutas real, lo usa; si no, cae automáticamente al motor simple de 6 carreteras.
Te falta generar ese grafo, una sola vez, en tu computadora (a partir del mismo
`cuba-latest.osm.pbf` que ya descargaste en el paso 1):

```bash
# Requiere Java 17+. Descarga el jar de GraphHopper (ajusta la versión si hay una más
# reciente en https://github.com/graphhopper/graphhopper/releases):
wget https://repo1.maven.org/maven2/com/graphhopper/graphhopper-web/10.2/graphhopper-web-10.2.jar

# Crea un archivo config.yml mínimo:
cat > config.yml << 'EOF'
graphhopper:
  graph.location: graph-cache
  profiles:
    - name: car
      vehicle: car
      weighting: fastest
EOF

# Importa el .osm.pbf y genera la carpeta graph-cache/ (puede tardar varios minutos):
java -Ddw.graphhopper.datareader.file=cuba-latest.osm.pbf -jar graphhopper-web-10.2.jar import config.yml
```

Copia la carpeta `graph-cache/` completa al teléfono, junto al `cuba.mbtiles`, en:

```
/Android/data/com.aistudio.cubagps.nvgtq/files/maps/graph-cache/
```

Con eso, `calculateRoute()` empieza a devolver rutas reales por todas las calles de Cuba,
con nombres de vía correctos y instrucciones giro-a-giro generadas por GraphHopper
(las traduje a español). Ya no depende de las 6 carreteras escritas a mano.

**Aviso:** no pude compilar ni probar este código aquí (sin acceso a internet ni a Android
Studio en este entorno). La arquitectura y el flujo de datos son correctos, pero la API de
GraphHopper cambia ligeramente entre versiones — si al abrir el proyecto en Android Studio
algún método aparece en rojo, es casi siempre un ajuste de nombre menor para la versión
exacta que Gradle resuelva; el propio autocompletado de Android Studio te lo señala.

## 6. Fase 3 — Descarga automática por WiFi (ya integrada)

Ya no hace falta copiar los archivos por USB. El botón de mapas offline (ícono 🗐 dentro
de la app) ahora descarga `cuba.mbtiles` y `graph-cache.zip` solo, en segundo plano, con
una notificación de progreso — y por defecto solo cuando hay WiFi (hay una casilla para
permitir datos móviles si el usuario lo prefiere). Si se corta la conexión a mitad de
camino, retoma desde donde se quedó en vez de volver a empezar.

Para que esto funcione, tú necesitas **alojar** los dos archivos en algún sitio con enlace
de descarga directa — la forma más simple y gratuita es un **GitHub Release** (hasta 2 GB
por archivo):

1. Sube `cuba.mbtiles` (del paso 2) y un `graph-cache.zip` (comprime la carpeta
   `graph-cache/` completa del paso 5) como assets de un Release en tu propio repositorio.
2. Copia `download.properties.example` a `download.properties` (en la raíz del proyecto;
   ese archivo NO se sube a git) y pega ahí las URLs directas de descarga de esos dos
   assets.
3. Compila la app normalmente — las URLs quedan incluidas en el APK.

Si dejas `download.properties` vacío o no lo creas, el botón de descarga simplemente no
aparece y sigue funcionando la opción de copiar los archivos manualmente por USB (Fase 1
y 2 de esta guía).

## 7. Fase 4 (futuro)

Con mapa 3D, rutas reales y descarga automática resueltos, lo que sigue de forma natural
es un modo "online" real: cuando haya datos móviles, usar un servidor de tiles/rutas
remoto en vez de depender solo de los archivos locales (útil para el día que actualicen
el mapa de Cuba en OSM y no quieras redistribuir el APK). Dime cuando quieras que
avancemos con eso.

## 8. Fase 4 — Modo online (funciona con o sin datos descargados)

**Mapa online — ya funciona, sin que hagas nada:** si el teléfono tiene internet pero
todavía no descargó `cuba.mbtiles`, la app usa automáticamente **OpenFreeMap**
(`tiles.openfreemap.org`), un servicio gratuito, sin API key y explícitamente pensado
para uso en producción (a diferencia de otros "demos"). No hay nada que configurar. Eso sí:
la versión online no incluye edificios en 3D (ese detalle solo viene con los datos locales
completos que generaste en la Fase 1).

**Rutas online — requiere que TÚ configures tu propio servidor, y aquí quiero ser
honesto:** existe un servidor público de demostración de OSRM (`router.project-osrm.org`),
pero su política de uso prohíbe explícitamente el uso "pesado" o de producción (límite de
~1 solicitud/segundo, "no es una API lista para producción"). Meterlo como backend por
defecto de una app real terminaría con la app bloqueada, así que **no lo integré**. En su
lugar, dejé el motor (`OnlineRoutingEngine.kt`) listo para hablar con **tu propio
servidor**, que es gratis de correr tú mismo:

```bash
# En tu PC o un servidor propio, con Docker, usando el mismo cuba-latest.osm.pbf:
docker run -t -v "${PWD}:/data" ghcr.io/project-osrm/osrm-backend osrm-extract -p /opt/car.lua /data/cuba-latest.osm.pbf
docker run -t -v "${PWD}:/data" ghcr.io/project-osrm/osrm-backend osrm-partition /data/cuba-latest.osrm
docker run -t -v "${PWD}:/data" ghcr.io/project-osrm/osrm-backend osrm-customize /data/cuba-latest.osrm
docker run -t -i -p 5000:5000 -v "${PWD}:/data" ghcr.io/project-osrm/osrm-backend osrm-routed --algorithm mld /data/cuba-latest.osrm
```

Luego expón ese puerto 5000 con HTTPS (por ejemplo detrás de un nginx con certificado) y
pon esa URL en `download.properties`:

```
ONLINE_ROUTING_BASE_URL=https://tu-servidor.com
```

Si dejas esa línea vacía, el ruteo online simplemente no se usa — la app sigue con
GraphHopper local si ya está descargado, o el motor de 6 carreteras como último recurso.
Con esto, el orden de prioridad para calcular una ruta queda: **1) GraphHopper local
(offline, el mejor) → 2) tu servidor OSRM online → 3) motor de respaldo.**

## 9. Aviso importante

El archivo `.mbtiles` de un país completo con edificios y POIs puede ser demasiado
grande para subir directo a Play Store dentro del APK (límite de 150 MB, o 200 MB con
Play Asset Delivery). Lo normal es distribuirlo como **descarga inicial** la primera
vez que el usuario abre la app con wifi — el código ya está preparado para eso (solo
falta que la app, al no encontrar el archivo, lo descargue de una URL tuya en vez de
fallar en silencio).
