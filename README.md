# API — Software para la gestión de objetos perdidos (UNIVA)

API REST en **Java 21 + Spring Boot 4.1** para la plataforma de objetos perdidos de la materia
Calidad de Software. Implementa los endpoints de la sección 5.1 de la especificación del primer
parcial, sobre la base de datos PostgreSQL de `database/base_datos_proyecto.sql`.

## Requisitos

- JDK 21
- PostgreSQL 14 o superior instalado en tu computadora (con pgAdmin 4 es lo más fácil).

## Base de datos local

1. En pgAdmin, sobre *Databases* → *Create* → *Database…*, crea `objetos_perdidos`
   (en *Definition*: Encoding `UTF8`, Template `template0`).
2. Clic derecho en `objetos_perdidos` → *Query Tool*, abre `database/base_datos_proyecto.sql` y ejecútalo
   completo (F5). Debe terminar sin errores y mostrar las consultas de verificación.

Con psql sería:

```cmd
psql -U postgres -c "CREATE DATABASE objetos_perdidos WITH ENCODING 'UTF8' TEMPLATE template0"
psql -U postgres -d objetos_perdidos -f database/base_datos_proyecto.sql
```

El script se puede volver a ejecutar para dejar los datos de prueba como al inicio.

## Variables de entorno

| Variable | Obligatoria | Ejemplo / valor por defecto |
|---|---|---|
| `spring_datasource_password` | Sí | La contraseña de tu usuario `postgres` local |
| `spring_datasource_url` | No | `jdbc:postgresql://localhost:5432/objetos_perdidos` |
| `spring_datasource_username` | No | `postgres` |
| `APP_JWT_SECRET` | Sí | Texto aleatorio de **al menos 32 caracteres** |
| `APP_UPLOADS_DIR` | No | `uploads` (carpeta donde se guardan las fotografías) |
| `APP_CORS_ORIGINS` | No | `http://localhost:5173,http://localhost:3000,http://localhost:4200` |

## Ejecutar

Windows (CMD):

```cmd
set spring_datasource_password=TU_CONTRASEÑA_DE_POSTGRES
set APP_JWT_SECRET=secreto-local-de-desarrollo-con-32-caracteres-o-mas
mvnw.cmd spring-boot:run
```

Mac / Linux: `export` en lugar de `set` y `./mvnw spring-boot:run`.
La API queda en http://localhost:8080. Las pruebas (`mvnw.cmd test`) no necesitan base de datos.

En IntelliJ: abre el proyecto, define las variables de entorno en la configuración de ejecución de
`ObjetosPerdidosApplication` y ejecútala.

`spring.jpa.hibernate.ddl-auto=validate`: si la API no arranca con un error de *schema validation*,
la base no se creó con el script de `database/`.

Las fotografías de los datos de prueba (`objeto_01.jpg` … `objeto_06.png`) están en
`uploads/objetos/` para que el cliente las muestre desde el primer arranque.

## Autenticación (JWT)

1. El cliente envía el correo a `POST /api/usuarios/identificacion` (público).
2. La respuesta trae los datos de PNT-02 y un `token` (vigencia de 8 horas).
3. En todas las demás llamadas se envía el encabezado `Authorization: Bearer {token}`.

El token lleva el id y el rol del usuario, así que la API sabe quién registra un objeto, quién
reclama y quién libera una petición sin que el cliente lo envíe. La identificación sigue siendo
solo por correo, como dice la especificación (no hay contraseñas en la base).

Permisos por rol:

| Acción | Estudiante | Administrador |
|---|---|---|
| Catálogos y consulta de objetos | ✓ | ✓ |
| Crear petición (reclamo o reclamo de error) | ✓ | — |
| Alta, edición y eliminación de objetos | — | ✓ |
| Consultar, contactar y liberar peticiones | — | ✓ |

Las fotografías (`GET /uploads/objetos/...`) son públicas para poder usarlas directo en `<img src>`.

## Endpoints

Base: `http://localhost:8080`

| Pantalla | Método | Ruta | Rol |
|---|---|---|---|
| PNT-01 | POST | `/api/usuarios/identificacion` | público |
| PNT-03, 05, 07 | GET | `/api/catalogos/{tipo}` — `tipos-objeto`, `colores`, `etiquetas`, `modulos` | ambos |
| PNT-03, 05 | GET | `/api/objetos?estado=PERDIDO&tipoObjetoId=1&colorId=2&otrasEtiquetaIds=1,4` | ambos |
| PNT-09 | GET | `/api/objetos?estado=RECLAMADO` | ambos |
| PNT-04, 06, 09 | GET | `/api/objetos/{id}` | ambos |
| PNT-07 | POST | `/api/objetos` (multipart/form-data) | admin |
| PNT-06 | PUT | `/api/objetos/{id}` (multipart/form-data) | admin |
| PNT-06 | DELETE | `/api/objetos/{id}` | admin |
| PNT-04 | POST | `/api/peticiones` | estudiante |
| PNT-08 | GET | `/api/peticiones?matricula=A2022087` | admin |
| PNT-08 | GET | `/api/peticiones/{id}` | admin |
| PNT-08 | GET | `/api/peticiones/{id}/contacto-previo` | admin |
| PNT-08 | PUT | `/api/peticiones/{id}/liberacion` | admin |

### Identificación

```http
POST /api/usuarios/identificacion
Content-Type: application/json

{ "correo": "ana.torres@alumnos.univa.mx" }
```

```json
{
  "usuario": { "id": 3, "nombre": "Ana Torres Gómez", "correo": "ana.torres@alumnos.univa.mx",
               "matricula": "A2023001", "rol": "ESTUDIANTE" },
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "tipoToken": "Bearer",
  "expiraEnSegundos": 28800
}
```

Correos de prueba: `laura.mendez@univa.mx` y `jorge.salas@univa.mx` (administradores);
`ana.torres@`, `luis.ramirez@`, `maria.ibarra@` y `carlos.nunez@alumnos.univa.mx` (estudiantes).

### Consulta de objetos

- **Estudiante:** `estado` es obligatorio (`PERDIDO` o `RECLAMADO`). Si no lo envía, la API responde 400
  con «Selecciona «Perdidos» o «Reclamados» para ver los objetos.»
- **Administrador:** si no envía `estado`, recibe los `PERDIDO` (su pantalla principal).
- `otrasEtiquetaIds`: el objeto debe tener **todas** las etiquetas seleccionadas. Se puede repetir
  (`?otrasEtiquetaIds=1&otrasEtiquetaIds=4`) o separar por comas (`?otrasEtiquetaIds=1,4`).
- Orden: perdidos del más reciente al más antiguo; reclamados por fecha de reclamo (más reciente primero).
- Una lista vacía (`[]`) significa «No hay objetos que coincidan…» o «Aún no hay objetos…».

```json
{
  "id": 1, "nombre": "Mochila negra con bolsillo lateral",
  "descripcion": "Mochila de dos compartimentos con bolsillo lateral para botella.",
  "marca": "JanSport", "lugarPerdida": "Cafetería central",
  "rutaImagen": "/uploads/objetos/objeto_01.jpg", "estado": "PERDIDO",
  "tipoObjeto": { "id": 1, "nombre": "Mochila", "descripcion": "..." },
  "color": { "id": 1, "nombre": "Negro", "codigoHex": "#000000" },
  "modulo": { "id": 1, "nombre": "Módulo A", "ubicacion": "...", "horarioAtencion": "Lun a Vie 8:00–20:00" },
  "otrasEtiquetas": [ { "id": 2, "nombre": "Con llavero" }, { "id": 4, "nombre": "Con estampas" } ],
  "fechaRegistro": "2026-11-24T10:14:01", "fechaReclamo": null
}
```

La imagen se muestra con `http://localhost:8080` + `rutaImagen`. La tarjeta usa `fechaRegistro`
(«Perdido el») o `fechaReclamo` («Reclamado el»). La respuesta nunca incluye quién reclamó el objeto.

### Alta y edición de objeto (multipart/form-data)

Campos: `fotografia` (archivo JPG/PNG, máx. 5 MB; obligatoria en el alta, opcional en la edición),
`nombre`, `descripcion`, `marca` (opcional), `tipoObjetoId`, `colorId`, `otrasEtiquetaIds` (repetido,
opcional), `lugarPerdida`, `moduloId`. La fecha de registro la asigna la base.

```js
const datos = new FormData();
datos.append('fotografia', archivo);            // <input type="file">
datos.append('nombre', 'Mochila negra');
datos.append('descripcion', 'Mochila con bolsillo lateral para botella.');
datos.append('tipoObjetoId', 1);
datos.append('colorId', 1);
datos.append('moduloId', 1);
datos.append('lugarPerdida', 'Cafetería central');
[2, 4].forEach(id => datos.append('otrasEtiquetaIds', id));

await fetch('http://localhost:8080/api/objetos', {
  method: 'POST',                                // PUT /api/objetos/{id} para editar
  headers: { Authorization: `Bearer ${token}` }, // NO pongas Content-Type: el navegador lo arma
  body: datos,
});
```

La API comprueba el contenido real del archivo, no solo la extensión. Eliminar es **borrado lógico**
(`estatus = FALSE`): el objeto deja de aparecer, pero se conserva en la base.

### Peticiones

```http
POST /api/peticiones
Authorization: Bearer {token de estudiante}
Content-Type: application/json

{ "objetoId": 1, "tipo": "RECLAMO" }
```

- El **tipo** lo decide la API según el estado del objeto (PERDIDO → `RECLAMO`, RECLAMADO →
  `RECLAMO_ERROR`). Enviarlo es opcional; si se envía y ya no coincide, la API responde 409
  «Este objeto cambió de estado…», lo que cubre el caso de F-03.
- La **matrícula** se toma del token, así que no se envía.

**Liberar** (`PUT /api/peticiones/{id}/liberacion`, sin cuerpo):

- **Reclamo:** el objeto pasa a `RECLAMADO` a nombre del estudiante y entra al historial. Los demás
  reclamos pendientes de ese objeto se desactivan solos. Si alguno de esos estudiantes era el dueño,
  puede levantar un reclamo de error.
- **Reclamo de error:** el caso se resuelve a favor de quien reportó. El objeto sigue `RECLAMADO`,
  pero ahora a su nombre, con la nueva fecha de reclamo. La entrega anterior queda en su propia petición.

**Contactar estudiante** (`GET /api/peticiones/{id}/contacto-previo`): solo en reclamos de error.
Devuelve nombre, matrícula, correo y teléfono de quien tenía el objeto antes del reporte, más el aviso
«Estos datos de contacto son solo para resolver este caso.»

## Formato de errores

Todas las respuestas de error tienen la misma forma. `mensaje` ya trae el texto de la especificación
para mostrarlo tal cual; `campos` aparece cuando hay errores por campo.

```json
{
  "status": 400,
  "error": "Bad Request",
  "mensaje": "Completa los campos obligatorios marcados con * para registrar el objeto.",
  "campos": { "nombre": "El nombre debe tener entre 3 y 60 caracteres.", "moduloId": "Selecciona el módulo de resguardo." },
  "timestamp": "2026-11-26T10:30:00"
}
```

| Código | Cuándo |
|---|---|
| 400 | Datos inválidos (correo, matrícula, campos del alta, imagen, estado sin elegir) |
| 401 | Sin token o token vencido: el cliente debe volver a PNT-01 |
| 403 | El rol no puede hacer esa acción, o la cuenta está desactivada |
| 404 | Correo, objeto o petición no encontrados |
| 409 | Petición duplicada, objeto que cambió de estado, petición ya liberada |
| 413 | Imagen de más de 5 MB |

## Estructura

```
src/main/java/mx/univa/objetosperdidos
├── config/       propiedades, carpeta de imágenes, reloj
├── security/     JWT, reglas por rol, CORS
├── entity/       entidades JPA (una por tabla) y enums
├── repository/   Spring Data JPA y búsqueda con filtros
├── dto/          objetos de entrada y salida (sección 5.2)
├── service/      reglas de negocio
├── controller/   endpoints REST
└── exception/    mensajes de la especificación y manejo de errores
database/         script DDL + DML de la base
uploads/objetos/  fotografías
```
