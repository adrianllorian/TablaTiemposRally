# Tabla Tiempos Rally — Análisis funcional

> Documento de trabajo. Recoge las decisiones cerradas con el usuario antes de programar.
> Aplicación web para llevar los tiempos de un rally de Assetto Corsa en vivo, sustituyendo el Excel + Skype por una tabla compartida.

---

## 1. Visión

Una única pantalla que sea a la vez la pizarra del rally, la hoja de tiempos y el orden de salida. Todos los participantes abren la URL, entran con una contraseña compartida y ven los tiempos actualizarse sin refrescar.

La app automatiza lo que en el Excel se hacía a mano: pintar celdas, sumar tramos, ordenar la clasificación, calcular diferencias y copiar columnas.

- **Nombre:** Tabla Tiempos Rally
- **Idioma:** español
- **Tema:** claro por defecto, con verdes suaves, crema y blanco. Tema oscuro opcional.
- **Sin PWA.**
- **Sin tooltips:** toda la información importante se ve en la tabla, en la cabecera del tramo o en el panel de detalle.

---

## 2. Alcance de la primera versión

- Login con contraseña compartida.
- Un rally activo y un historial de rallies.
- Configuración del rally: nombre, fecha, tramos, pilotos y coches, shakedown opcional, modo de penalización.
- Tabla única para editar, consultar y mostrar el orden de salida.
- Colores automáticos, morado, récord histórico y corona.
- Penalizaciones y retirados.
- Diferencias con el líder y con el anterior.
- Panel de detalle por tramo.
- Página de récords con top 5.
- Exportación manual a Excel.
- Enlace para invitar a visualizar, de solo lectura.
- Reloj y cuenta atrás de 20 s.
- Pantalla completa y mantener pantalla encendida.
- Autocompletado de tramos, coches y pilotos.
- Actualizaciones en vivo.

Fuera de alcance de la primera versión:

- PWA / instalación en móvil.
- Registro de cambios y auditoría.
- Copia de seguridad automática.
- Varios rallies activos a la vez.
- Añadir pilotos con el rally en marcha.
- Vista móvil específica de “mi tiempo”.
- Pegar bloques desde Excel.

---

## 3. Acceso y permisos

- Una sola contraseña, configurada en el primer arranque.
- La contraseña se valida en el backend. No se envía al frontend ni se expone en el JavaScript.
- Todos los que entran con la contraseña pueden editar todo: tiempos, configuración, pilotos, tramos, exportar y borrar.
- No hay nombre de usuario ni registro de quién hizo cada cambio.
- El enlace para invitar a visualizar es una URL secreta por rally, de solo lectura, sin historial ni récords.
- La sesión se recuerda con una cookie; hay botón de salir.
- El nombre de la app y el logo se configuran una vez en el despliegue, no por rally.

---

## 4. Modelo de datos conceptual

- **AppConfig:** nombre de la app, logo, hash de la contraseña.
- **Rally:** nombre, fecha, estado (activo / terminado), shakedown activo, nombre del shakedown, modo de penalización, valor de penalización, token de invitado.
- **Stage:** rally, orden, nombre, tipo (`TC` o `SHAKEDOWN`). El código `TC-1`, `TC-2`… se deriva del orden.
- **Entrant:** rally, orden de inscripción, nombre del piloto, coche, retirado.
- **Result:** tramo, piloto, tipo (`VÁLIDO`, `PENALIZACIÓN`), milisegundos, marca de tiempo.
- **Diccionarios:** nombres canónicos de tramos, coches y pilotos, para autocompletar.
- **Récords:** se calculan a partir de los resultados válidos agrupados por nombre canónico de tramo.

Los tiempos se guardan en milisegundos. El formato visible es `mm.ss.mmm` (`00.00.000`).

---

## 5. Reglas de negocio

### 5.1 Rally

- Solo puede haber **un rally activo** a la vez.
- **Nuevo rally:** botón en el historial. Crea un rally nuevo con los tiempos a cero. El rally actual pasa al historial. Solo queda uno activo.
- Crear rally: nombre, tramos, pilotos y coches, shakedown opcional y modo de penalización.
- **Guardar cambios** en la configuración actualiza el rally actual y **no borra los tiempos**. Para empezar de cero se usa **Nuevo rally**.
- Duplicar configuración: copia tramos, pilotos, coches y ajustes, pero empieza sin tiempos.
- Historial: se guardan todos los rallies. Se pueden consultar, duplicar, reabrir, exportar y borrar.
- Reabrir un rally terminado: vuelve a ser el activo. Si hay otro activo, se avisa y hay que cerrarlo o archivarlo.
- Borrar un rally: confirmación. Sus tiempos desaparecen también de los récords y se recalcula el top.
- La fecha se asigna automáticamente al crear el rally; no se edita.

### 5.2 Configuración con el rally en marcha

- Guardar cambios **no reinicia los tiempos**.
- No se añaden pilotos nuevos después de empezar el primer tramo.
- Se pueden corregir nombres de pilotos y coches.
- Se pueden renombrar tramos; la cabecera se actualiza.
- Se puede añadir un tramo al final o quitar un tramo sin tiempos con confirmación.
- Borrar un tramo con tiempos exige confirmación y recalcula totales, clasificación y récords.
- Borrar un piloto exige confirmación y elimina sus tiempos.

### 5.3 Tramos y shakedown

- El código `TC-1`, `TC-2`… lo pone la app. El nombre lo escriben los usuarios.
- La cabecera muestra `TC-1 Muncó`. Si no hay nombre, solo `TC-1`.
- El shakedown es opcional por rally.
- El shakedown:
  - Va siempre antes del TC-1.
  - Tiene nombre opcional para entrar en récords.
  - Se corre una sola vez.
  - No suma al rally.
  - Ordena la salida del TC-1.
  - Ordena la tabla hasta que se introduce el primer tiempo de TC-1.
  - El mejor se pinta de morado; si bate récord, lleva corona.
  - No tiene penalizaciones ni retirados: si alguien se sale, se queda sin tiempo.
  - Los que no lo corren van al final, en orden de inscripción.
  - Empate: gana el primer piloto inscrito.
  - La cabecera se muestra como `Shakedown · Muncó`.

### 5.4 Estados de un tramo

- **Pendiente:** no tiene ningún tiempo.
- **En curso:** tiene al menos un tiempo y todavía no están todos.
- **Cerrado:** todos los pilotos activos tienen tiempo, penalización o están retirados.
- **Anulado:** no hay ningún tiempo válido porque todos tienen penalización o están retirados. No suma al rally.

No hay botón de “iniciar tramo”: el tramo activo es el primero que no está cerrado.

### 5.5 Cierre de tramo

Un tramo se cierra cuando **todos los pilotos activos** tienen información en su columna: tiempo, penalización o retirado.

- Los retirados no bloquean el cierre.
- Un piloto sin tiempo sigue “en curso” y bloquea el cierre.
- Si alguien se queda sin conexión, se le espera. No se retira ni se penaliza automáticamente.
- La app muestra un aviso con los que faltan: `Faltan 2: Pepe, Juan`.

### 5.6 Tiempos

- Formato `mm.ss.mmm`: minutos, segundos y milésimas.
- Entrada rápida: `123456` se convierte en `01.23.456`.
- Validación estricta del formato.
- El `0` no es un tiempo válido. Celda vacía = sin tiempo.
- Aviso suave si se detecta un tiempo duplicado o sospechoso; no bloquea.
- Los tiempos se pueden editar siempre, también los de tramos cerrados.
- Editar un tramo cerrado recalcula la general y **reordena la tabla en ese momento**, aunque haya otro tramo en curso.
- La tabla se reordena en vivo según el total: las filas siguen la clasificación general.

### 5.7 Pilotaje: penalizaciones y retirados

- **Penalización:** se aplica a un piloto en un tramo y se pinta en rojo.
- Modos configurables a nivel de rally:
  - **Peor tiempo válido del tramo + X.** Por defecto X = 30 s. X se puede expresar en segundos o minutos.
  - **Tiempo fijo.** Se escribe en formato de tiempo.
- La penalización es dinámica: si aparece un tiempo válido peor, se recalcula.
- Si todavía no hay ningún tiempo válido, queda pendiente.
- Si todos los pilotos activos acaban con penalización o retirados y no hay ningún tiempo válido, el tramo se anula y no suma.
- La penalización sí cuenta para la clasificación del tramo y para las diferencias, porque es tiempo que se puede remontar.
- La referencia del tramo sigue siendo el mejor tiempo válido (el morado); el penalizado muestra su diferencia respecto a ese mejor.
- Se puede quitar una penalización.
- **Retirado:** estado final del piloto. No corre más tramos, no aparece en la general, no bloquea el cierre y se muestra en gris. Se puede deshacer con confirmación.

### 5.8 Colores

- **Morado:** mejor tiempo válido del tramo entre los pilotos activos.
- **Verde:** resto de tiempos válidos.
- **Rojo:** penalización.
- **Gris:** retirado, en curso o tramo anulado.
- **Corona:** récord histórico del nombre del tramo.
- **Punto / marca:** valor provisional hasta que el tramo se cierre.

Empate en el morado: gana el que salió antes al tramo. La app guarda el orden de salida de cada tramo para resolverlo aunque la tabla se haya reordenado.

Empate en la general: se mantiene el orden anterior (orden estable).

### 5.9 Orden y clasificación

- La tabla tiene **una sola vista**.
- Las filas están **siempre en el orden de la clasificación general**: de mejor a peor.
- Los pilotos **en curso** van al final, con su total parcial en gris y sin posición ni diferencia.
- Los **retirados** van al final y no aparecen en la general.
- El orden de salida de un tramo es la clasificación general al cerrar el tramo anterior.
- El orden de salida del TC-1 es el del shakedown, si existe; si no, el orden de inscripción.
- Al cerrar el tramo, la clasificación es también el orden de salida del siguiente.
- La columna `Pos` y las diferencias coinciden con el orden de las filas.
- Para el desempate del morado se guarda el orden de salida de cada tramo: gana el que salió antes.
- La general solo clasifica a los pilotos que ya tienen resultado en el tramo actual. Los demás están **en curso**: sin posición, sin diferencia y con el total anterior en gris. Así no se comparan 2 tramos con 3.
- Los tramos anulados no suman.

### 5.10 Diferencias

- **General:**
  - `Dif. líder`, siempre visible, en rojo.
  - `Dif. ant.`, siempre visible, en gris: diferencia con el piloto que está justo delante en la clasificación. Debajo se muestra la posición y el nombre de ese piloto de referencia.
- **Por tramo:**
  - Interruptor `Mostrar diferencias`.
  - Al activarlo, cada celda muestra una segunda línea: `+0.500 · +0.200 · 2º Carlos`.
  - Primero, diferencia con el mejor del tramo.
  - Segundo, diferencia con el piloto de delante en ese tramo.
  - Tercero, la posición y el nombre del piloto de referencia de esa diferencia anterior.
  - Una leyenda sobre la tabla explica el orden.
  - El panel del tramo muestra la clasificación ordenada con nombres, las dos diferencias y la referencia.
- Sin tooltips.
- Mientras el tramo está abierto, las diferencias son provisionales y llevan una marca.
- Las penalizaciones cuentan para las diferencias.
- Si el tramo se anula, no hay diferencias de tramo y la general no cambia.

### 5.11 Récords históricos

- Se guarda un récord por **nombre canónico de tramo**, no por código `TC-x`.
- `Subida a Muncó` es la misma ficha en cualquier rally.
- La comparación ignora mayúsculas y acentos.
- Entran los tramos con nombre y el shakedown con nombre.
- Solo cuentan tiempos válidos de tramos que computan. No cuentan penalizaciones ni tramos anulados.
- El récord se actualiza en vivo.
- Al batir el récord: aviso visible con el nombre del piloto. En el enlace de invitado también.
- Al empatar el récord: aviso `Igualaste el récord`; el dueño no cambia.
- La corona marca la celda que tiene el récord actual. Si el récord es de otro rally y nadie lo baja, no hay corona en la tabla actual; el récord se ve en la cabecera del tramo y en la página de récords.
- La cabecera del tramo muestra `🏆 2.10.500` con el récord a batir.
- El panel del tramo muestra el dueño, coche, rally y fecha.
- Al corregir o borrar un tiempo, los récords se recalculan.
- Al borrar un rally, sus tiempos desaparecen de los récords.
- Página de récords: con contraseña, buscador, récord + top 5 por tramo, con piloto, coche, rally y fecha. Etiqueta `SD` para shakedown.
- Hoja `Récords` en el Excel exportado.

### 5.12 Historial

- Lista de rallies con nombre, fecha, estado, ganador y número de tramos.
- Acciones: abrir, duplicar configuración, exportar, borrar.
- El rally activo aparece destacado.

### 5.13 Reloj y cuenta atrás

- Reloj actual en formato 24 h.
- Cuenta atrás de 20 s en bucle, sincronizada con el reloj real:
  - En los segundos `:00`, `:20` y `:40` del reloj, la cuenta atrás marca `0`.
  - Del `:01` al `:19`, del `:21` al `:39` y del `:41` al `:59`, baja de 19 a 1.
  - `0` en verde.
  - `5, 4, 3, 2, 1` en ámbar.
  - El resto en rojo.
- Sin sonido.
- Reloj sincronizado con el servidor para que todos vean el mismo segundo.

### 5.14 Pantalla

- Interruptor `Mantener pantalla encendida`, **desactivado por defecto**. Al activarlo se usa la API `Screen Wake Lock`.
- Se reactiva al volver a la pestaña.
- Si el navegador no lo permite, se avisa.
- Tema claro / oscuro.
- Aviso de `Reconectando` si se cae la conexión.

---

## 6. Interfaz

### 6.1 Login

- Logo, nombre de la app, campo de contraseña y botón de entrar.
- Contraseña configurada en el primer arranque.
- Sin selección de nombre.

### 6.2 Pantalla principal del rally

Cabecera:

- Logo y nombre de la app.
- Nombre y fecha del rally.
- Reloj y cuenta atrás.
- Botones: pantalla encendida, tema, invitar, configuración, exportar, salir.

Barra de estado:

- Chip del tramo activo: `TC-3 En curso`.
- Aviso de los que faltan: `Faltan 1: Mario`.
- Indicador de conexión.

Tabla única:

`Pos | Piloto | Coche | Shakedown | TC-1 … TC-n | Total | Dif. líder | Dif. ant.`

- Cabecera de tramo: código, nombre, récord y chip de estado.
- Celdas con tiempo, color, corona y segunda línea de diferencias si el interruptor está activo.
- Total, diferencia con el líder (rojo) y diferencia con el anterior (gris).
- Retirados en gris.
- En curso con total gris y `Pos`/`Dif` a `—`.
- Leyenda fija: morado, verde, rojo, corona, provisional, en curso, retirado.
- Scroll horizontal: `Pos`, `Piloto` y `Coche` quedan fijos a la izquierda; `Total`, `Dif. líder` y `Dif. ant.` quedan fijos a la derecha.
- Al abrir el rally, la tabla se centra en el tramo en curso.

### 6.3 Panel de tramo

Se abre al tocar la cabecera de un tramo.

- Nombre y estado del tramo.
- Récord histórico con dueño, coche, rally y fecha.
- Clasificación del tramo: posición, piloto, coche, tiempo, diferencia con el mejor, diferencia con el anterior y referencia del anterior.
- Los que aún corren, abajo como `en curso`.
- Sin tooltips.

### 6.4 Página de récords

- Con contraseña.
- Buscador por nombre de tramo.
- Ficha por tramo: récord, dueño, coche, rally, fecha y top 5.
- Etiqueta `SD` para shakedown.
- Sin tooltips.

### 6.5 Página de historial

- Lista de rallies.
- Estado, ganador, fecha y acciones.
- Botón **Nuevo rally**.
- Acción **Duplicar** para crear otro rally con la misma configuración y los tiempos a cero.
- Rally activo destacado.

### 6.6 Configuración

- Nombre del rally.
- Fecha automática, no editable; se asigna al crear el rally.
- Tramos: código automático + nombre.
- Shakedown: activar/desactivar + nombre.
- Pilotos: nombre + coche.
- Penalización: peor + X (por defecto 30 s) o tiempo fijo.
- Autocompletado de tramos, coches y pilotos; se pueden escribir nombres nuevos.
- Guardar cambios no reinicia los tiempos. Para empezar de cero, **Nuevo rally**.

### 6.7 Enlace para invitar a visualizar

- Botón `Invitar` en la cabecera, con la URL y un botón `Copiar enlace`.
- URL secreta por rally.
- Quien abre el enlace ve la misma tabla, en grande, en solo lectura.
- Sin contraseña.
- Reloj y cuenta atrás.
- Aviso de récord.
- Sin historial, sin récords, sin configuración.

---

## 7. Tiempo real y arquitectura

- **Backend:** Spring Boot 3, un solo JAR ejecutable.
- **Base de datos:** H2 en archivo, junto al JAR. Sin base de datos aparte.
- **Frontend:** Angular compilado y servido dentro del JAR.
- **Tiempo real:** SSE (Server-Sent Events). El servidor calcula todo y envía el estado completo del rally con un número de versión. El cliente se resincroniza al reconectar.
- **Autoridad:** el servidor calcula colores, orden, totales, diferencias y récords. El cliente solo renderiza.
- **Despliegue:** servidor de casa + DuckDNS + HTTPS (Caddy o Nginx). Se configura al final.
- **Exportación:** Apache POI para generar el Excel.

---

## 8. Fases

1. **Análisis y maqueta estática.** Este documento y la maqueta navegable.
2. **Backend:** modelo, base de datos, configuración de rally, tiempos, reglas, SSE.
3. **Frontend:** login, tabla, edición, colores, reloj, panel de tramo, diferencias.
4. **Récords, historial, exportación, enlace de invitado, tema oscuro, wake lock.**
5. **Despliegue** en el servidor de casa con DuckDNS y HTTPS.

---

## 9. Criterios de aceptación

- Varios usuarios ven los mismos tiempos sin refrescar.
- La tabla está siempre ordenada por el total, también con un tramo en curso.
- El morado, el verde, el rojo y la corona son correctos.
- Un tramo no se cierra hasta que todos los activos tienen tiempo, penalización o retirado.
- Un tramo con todos penalizados no suma.
- Los retirados no aparecen en la general.
- Las diferencias generales y por tramo son correctas.
- El récord histórico se actualiza, se recalcula al corregir y se ve en la página de récords.
- El shakedown no suma y ordena el TC-1.
- La exportación a Excel incluye colores, clasificación y récords.
- El enlace de invitado es de solo lectura.
- La pantalla se mantiene encendida si se activa el interruptor y el navegador lo permite.
