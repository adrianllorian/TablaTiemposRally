# Tabla Tiempos Rally

Aplicación web para llevar los tiempos de un rally de **Assetto Corsa** en directo, sustituyendo el clásico Excel compartido por Skype.

Una sola URL para todos: los participantes entran con una contraseña, ven la tabla actualizarse en vivo y pueden editar tiempos, penalizaciones y retiradas. Incluye clasificación general, diferencias, récords históricos, historial de rallies, enlace de invitado en solo lectura y exportación a Excel.

> Creado por **Bochi**. Proyecto no oficial, sin relación con Kunos Simulazioni.

---

## Índice

- [Características](#características)
- [Descargar y ejecutar](#descargar-y-ejecutar)
- [Datos y copias de seguridad](#datos-y-copias-de-seguridad)
- [Compartir con los participantes](#compartir-con-los-participantes)
- [Stack técnico](#stack-técnico)
- [Desarrollo](#desarrollo)
- [Compilar el JAR definitivo](#compilar-el-jar-definitivo)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Cómo funciona](#cómo-funciona)
- [API principal](#api-principal)
- [Contribuir](#contribuir)
- [Publicar una versión en GitHub](#publicar-una-versión-en-github)
- [Preguntas frecuentes](#preguntas-frecuentes)
- [Licencia](#licencia)

---

## Características

- **Un único rally activo** y un historial con todos los rallies anteriores.
- **Tabla en vivo** mediante SSE: los cambios aparecen sin recargar.
- **Clasificación automática** por tiempo total, con `EC` (en curso) y retirados al final.
- **Tramos**: tiempos válidos, penalizaciones y retiradas.
- **Penalizaciones dinámicas**:
  - peor tiempo válido del tramo + X (30 s por defecto),
  - o tiempo fijo.
- **Colores automáticos**:
  - morado: mejor tiempo válido del tramo,
  - verde: tiempo válido,
  - rojo: penalización,
  - gris: en curso, retirado o tramo anulado,
  - corona: récord histórico.
- **Récords por nombre de tramo**, no por `TC-x`, con top 5, buscador y actualización en vivo.
- **Shakedown opcional** con nombre: entra en récords, pero no suma al rally.
- **Diferencias** con el líder y con el piloto anterior, con referencia de posición y nombre.
- **Reloj y cuenta atrás de 20 s** sincronizados con el reloj real.
- **Enlace de invitado** por rally, de solo lectura, sin contraseña.
- **Exportación a Excel** con clasificación, colores y hoja de récords.
- **Configuración inicial** en el primer arranque: contraseña y logo de la aplicación.
- **Cambio de contraseña y logo** desde `Configuración → Ajustes de la aplicación`.
- **Tema claro y oscuro**, diseño responsive y aviso de reconexión.
- **Sin PWA**, sin instalación y sin tooltips: todo se ve en la tabla.

---

## Descargar y ejecutar

### Requisitos

- **Java 21** o superior. Comprueba con:

  ```bash
  java -version
  ```

  Si no lo tienes, instala un JDK 21 (Temurin, Oracle, Microsoft OpenJDK, etc.).

### Pasos

1. Entra en la pestaña **Releases** de este repositorio.
2. Descarga el archivo `tabla-tiempos-*.jar`.
3. Crea una carpeta para la aplicación, por ejemplo `TablaTiempos`, y copia ahí el JAR.
4. Abre una terminal en esa carpeta y ejecuta:

   ```bash
   java -jar tabla-tiempos-1.0.0.jar
   ```

5. Abre en el navegador:

   ```text
   http://localhost:8888
   ```

6. En el primer arranque la app pide:
   - la **contraseña** que se usará para entrar,
   - el **logo** de la aplicación.

   El logo y la contraseña se guardan para toda la aplicación. Después se pueden cambiar en `Configuración → Ajustes de la aplicación`.

Para detener el servidor, pulsa `Ctrl + C` en la terminal.

### Cambiar el puerto

Por defecto usa el `8888`. Puedes cambiarlo al arrancar:

```bash
java -jar tabla-tiempos-1.0.0.jar --server.port=9090
```

También se puede fijar en `backend/src/main/resources/application.properties` si compilas desde el código.

---

## Datos y copias de seguridad

La base de datos es **H2 en archivo** y se guarda junto al directorio desde el que arrancas la aplicación:

```text
data/
├── tabla-tiempos.mv.db     # base de datos
└── logo/                   # logo subido desde la app
```

- Para hacer una copia de seguridad, **para la aplicación** y copia la carpeta `data/`.
- Para restaurarla, deja la carpeta `data/` junto al JAR y arranca de nuevo.
- Para empezar de cero (borrar rallies, récords, contraseña y logo), para la aplicación y elimina la carpeta `data/`.
- No subas la carpeta `data/` a GitHub: contiene la contraseña cifrada, los rallies y el logo.

---

## Compartir con los participantes

### En la misma red local

1. Averigua la IP local del equipo que ejecuta el servidor.
2. Abre el puerto `8888` en el cortafuegos de Windows.
3. Comparte:

   ```text
   http://IP_DEL_SERVIDOR:8888
   ```

### Por internet

Para que funcione fuera de casa, publica el puerto mediante un proxy inverso con **HTTPS** (Caddy, Nginx, Cloudflare Tunnel, etc.). No expongas el puerto `8888` directamente a internet sin HTTPS.

Dentro de la app, el botón **Invitar** genera un enlace de solo lectura para ese rally:

```text
https://tu-dominio/#/ver/TOKEN
```

Quien lo abra verá la tabla en directo, sin contraseña, sin configuración, sin historial y sin récords.

---

## Stack técnico

| Capa | Tecnología |
| --- | --- |
| Frontend | Angular 22 (standalone components, signals) |
| Backend | Spring Boot 4.1.2, Java 21 |
| Base de datos | H2 en archivo |
| Tiempo real | Server-Sent Events (SSE) |
| Autenticación | Sesión con cookie HttpOnly + BCrypt |
| Exportación | Apache POI (`.xlsx`) |
| Build | Maven Wrapper + Angular CLI |

---

## Desarrollo

### Requisitos

- **JDK 21**.
- **Node.js 22 o superior** y npm (el proyecto usa npm 11).
- Git.

### 1. Backend

```bash
cd backend
```

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux / macOS:

```bash
./mvnw spring-boot:run
```

El backend queda en `http://localhost:8888`.

### 2. Frontend en modo desarrollo

En otra terminal:

```bash
cd frontend
npm install
npm start
```

Abre `http://localhost:4200`. El servidor de Angular redirige `/api` al backend del `8888` mediante `proxy.conf.json`, así que los cambios del frontend se recargan al instante.

### 3. Tests

```bash
cd backend
./mvnw test
```

Windows:

```powershell
cd backend
.\mvnw.cmd test
```

```bash
cd frontend
npm test
```

---

## Compilar el JAR definitivo

El JAR incluye el frontend compilado dentro de `backend/src/main/resources/static`, de modo que un solo archivo sirve toda la aplicación.

### 1. Compilar el frontend

```bash
cd frontend
npm ci
npm run build
```

El resultado queda en `frontend/dist/frontend/browser`.

### 2. Copiarlo al backend

Windows (PowerShell):

```powershell
Remove-Item -Recurse -Force backend/src/main/resources/static/*
Copy-Item -Recurse -Force frontend/dist/frontend/browser/* backend/src/main/resources/static/
```

Linux / macOS:

```bash
rm -rf backend/src/main/resources/static/*
cp -R frontend/dist/frontend/browser/. backend/src/main/resources/static/
```

### 3. Empaquetar

```bash
cd backend
./mvnw -DskipTests package
```

Windows:

```powershell
.\mvnw.cmd -DskipTests package
```

El JAR se genera en:

```text
backend/target/tabla-tiempos-1.0.0.jar
```

Y se ejecuta igual que el descargado:

```bash
java -jar backend/target/tabla-tiempos-1.0.0.jar
```

---

## Estructura del proyecto

```text
TablaTiemposAssetoCorsa/
├── backend/                  # Spring Boot
│   ├── src/main/java/        # dominio, servicios, controladores, SSE
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── static/           # frontend compilado que sirve el JAR
│   ├── mvnw / mvnw.cmd
│   └── pom.xml
├── frontend/                 # Angular
│   ├── src/app/              # componentes, servicios, modelos y pipes
│   ├── src/styles.css
│   ├── proxy.conf.json
│   └── package.json
├── docs/
│   └── analisis.md           # especificación funcional del proyecto
├── mockup/
│   └── index.html            # maqueta visual original
├── tools/                    # JDK local de desarrollo (no se sube)
└── README.md
```

---

## Cómo funciona

- El **frontend Angular** se sirve desde el propio Spring Boot en producción.
- El backend expone una API REST protegida por sesión.
- La tabla se actualiza por **SSE** (`/api/rallies/{id}/events`): al guardar un tiempo, el servidor recalcula la clasificación y envía la vista completa a todos los clientes conectados.
- Los cálculos de tiempos, penalizaciones, diferencias, colores, récords y estados se hacen **en el servidor** (`RallyViewService`, `RecordService`).
- La contraseña se guarda como hash **BCrypt** en la tabla `app_settings`; nunca se guarda en texto plano ni se envía al frontend.
- El **logo** se guarda en `data/logo/` y se sirve desde `/api/public/logo`.
- El **primer arranque** se detecta con `/api/public/status`; si no hay configuración, la app muestra el formulario de contraseña y logo.
- El **enlace de invitado** usa un token aleatorio por rally y solo permite lectura.

---

## API principal

| Método | Ruta | Descripción |
| --- | --- | --- |
| `POST` | `/api/auth/login` | Iniciar sesión con la contraseña |
| `POST` | `/api/auth/logout` | Cerrar sesión |
| `GET` | `/api/auth/me` | Comprobar sesión |
| `GET` | `/api/public/status` | Estado de configuración inicial |
| `POST` | `/api/public/setup` | Configurar contraseña y logo la primera vez |
| `GET` | `/api/public/logo` | Logo de la aplicación |
| `GET` | `/api/rallies` | Listado e historial de rallies |
| `POST` | `/api/rallies` | Crear rally |
| `GET` | `/api/rallies/{id}` | Vista completa del rally |
| `PUT` | `/api/rallies/{id}/config` | Editar configuración |
| `PUT` | `/api/rallies/{id}/results/{stageId}/{entrantId}` | Guardar o borrar un resultado |
| `POST` | `/api/rallies/{id}/entrants/{entrantId}/retire` | Retirar piloto |
| `DELETE` | `/api/rallies/{id}/entrants/{entrantId}/retire` | Deshacer retirada |
| `POST` | `/api/rallies/{id}/reopen` | Reabrir un rally terminado |
| `GET` | `/api/rallies/{id}/export` | Exportar el rally a Excel |
| `GET` | `/api/rallies/{id}/events` | SSE con la tabla en vivo |
| `GET` | `/api/records` | Récords históricos |
| `GET` | `/api/dictionaries` | Tramos, coches y pilotos para autocompletar |
| `GET` | `/api/settings` | Ajustes de la aplicación |
| `PUT` | `/api/settings/password` | Cambiar la contraseña |
| `POST` | `/api/settings/logo` | Cambiar el logo |
| `GET` | `/api/public/rallies/{token}` | Vista de invitado en solo lectura |

---

## Contribuir

Las contribuciones son bienvenidas, sobre todo si has usado la app en un rally real y has encontrado un fallo o una mejora clara.

### Antes de empezar

- Para cambios grandes, abre primero un **issue** y explica qué quieres cambiar.
- Para fallos, incluye:
  - qué esperabas que pasara,
  - qué pasó realmente,
  - pasos para reproducirlo,
  - navegador y sistema operativo,
  - captura o texto del error si lo hay.

### Flujo de trabajo

1. Haz un fork del repositorio.
2. Crea una rama:

   ```bash
   git checkout -b feat/mi-mejora
   ```

   o

   ```bash
   git checkout -b fix/mi-correccion
   ```

3. Haz los cambios y pruébalos en local.
4. Comprueba que compilan backend y frontend:

   ```bash
   cd backend && ./mvnw -DskipTests package
   cd ../frontend && npm run build
   ```

5. Abre una **pull request** explicando el cambio y cómo lo has probado.

### Criterios del proyecto

- La interfaz está en **español**.
- **Sin tooltips**: la información importante debe verse en la tabla, la cabecera del tramo o el panel de detalle.
- Mantener los colores y el estilo visual existentes.
- Los cálculos de clasificación, diferencias, penalizaciones y récords van en el **backend**.
- Angular con componentes **standalone** y **signals**.
- No añadir dependencias sin necesidad.
- No subir a Git:
  - `backend/data/` (base de datos y logo),
  - `backend/target/`,
  - `frontend/node_modules/`, `frontend/dist/` ni `frontend/.angular/`,
  - `tools/` (JDK local),
  - el `.jar` compilado.

---

## Publicar una versión en GitHub

**No es recomendable hacer `git add` del JAR.** Aunque quepa en GitHub, cada compilación añade unos 70 MB al historial del repositorio y acaba haciendo el clonado muy pesado.

Lo recomendado es adjuntar el JAR a una **Release**:

1. Compila el JAR definitivo (ver arriba).
2. Crea un tag:

   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

3. En GitHub, entra en **Releases → Draft a new release**, selecciona el tag y adjunta el archivo:

   ```text
   backend/target/tabla-tiempos-1.0.0.jar
   ```

4. Publica la release.

Así los usuarios pueden descargar el JAR desde **Releases** sin llenar el repositorio de binarios, y el código fuente sigue siendo ligero para quien quiera contribuir.

También se puede automatizar con GitHub Actions en el futuro: compilar en cada tag y adjuntar el JAR a la release automáticamente.

---

## Preguntas frecuentes

**¿Dónde está la contraseña?**  
No está en el código. Se elige en el primer arranque y se guarda cifrada con BCrypt en la base de datos.

**¿Puedo cambiar la contraseña o el logo?**  
Sí: `Configuración → Ajustes de la aplicación`.

**¿Cuántos rallies puede haber a la vez?**  
Solo uno activo. Al crear otro, el anterior pasa al historial y se puede consultar, duplicar, reabrir, exportar o borrar.

**¿Se puede ver sin contraseña?**  
Sí, con el enlace de invitado de cada rally. Es de solo lectura y no da acceso a configuración, historial ni récords.

**¿Funciona en el móvil?**  
La interfaz es responsive y se puede usar desde el navegador del móvil. No hay una vista móvil específica de “mi tiempo”.

**¿Es una PWA?**  
No. Es una aplicación web normal: necesita el servidor encendido.

**¿Los datos se pierden al actualizar el JAR?**  
No, si mantienes la carpeta `data/` junto al nuevo JAR.

**¿Cómo hago una copia de seguridad?**  
Para la aplicación y copia la carpeta `data/`.

**¿La exportación a Excel incluye los récords?**  
Sí, el `.xlsx` tiene una hoja `Rally` con la clasificación y una hoja `Récords` con el top 5 por tramo.

**¿Se puede usar sin internet?**  
Sí, en local o en red local. Para jugar con amigos remotos necesitas publicar el servidor con HTTPS.

---

## Licencia

Este proyecto se distribuye bajo la **licencia MIT**. Puedes usar, copiar, modificar y compartir el código, siempre que mantengas el aviso de copyright y el texto de la licencia.

Consulta el archivo [`LICENSE`](LICENSE) para ver el texto completo.

---

Hecho con cariño para los rallies de Assetto Corsa. 🏁
