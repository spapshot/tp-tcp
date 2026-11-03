# TP Sockets TCP en Java — Java Socket Messaging System (JSMS)

Resolución de la *Guía de Ejercitación Java Sockets TCP*, siguiendo las plantillas del
Manual de Codificación y los conceptos de la Guía Didáctica.

## Estructura

| Carpeta | Contenido |
|---|---|
| `ejercicio1/` | `EchoServer` (mono-hilo) y `EchoClient` |
| `ejercicio2/` | `MultiClientServer` + `ClientHandler` (eco) con `ExecutorService` |
| `ejercicio3/` | `Command`, `ProtocolParser`, `ClientHandler` con CONNECT / JOIN / MSG / QUIT (sin broadcast) |
| `ejercicio4_5/` | Sistema completo: `RoomManager`, `ClientHandler`, `JsmsServer`, `JsmsClient`, parser |

## Compilar y ejecutar (JDK 11+)

```bash
cd ejercicio4_5
javac *.java
java JsmsServer            # terminal 1  (puerto 8080)
java JsmsClient            # terminal 2, 3, ...
```
Para probar sin cliente Java: `nc localhost 8080`. Los ejercicios 1 a 3 se compilan y corren igual desde su carpeta
(`EchoServer`/`EchoClient` en la 1, `MultiClientServer` en la 2 y 3).

Opciones de prueba del servidor completo: `-Djsms.port=9090` y `-Djsms.timeout=5000` (por defecto 8080 y 30000 ms).

## Protocolo (cada mensaje termina en `\n`)

| Comando | Respuesta |
|---|---|
| `CONNECT <user>` | `200 OK` o `401 ERROR ...` (nick repetido / ya conectado) |
| `JOIN <sala>` | `JOINED <sala>` (si estaba en otra sala, la abandona) |
| `MSG <texto>` | Los demás miembros de la sala reciben `MSG <user>: <texto>`; el emisor recibe `200 OK` (sin eco propio) |
| `QUIT` | `BYE` y cierre limpio |
| Sintaxis inválida | `ERROR <descripción>` sin cerrar el socket |

Mensajes del servidor: `INFO <user> se unio a/salio de/se desconecto`, `TIMEOUT ...`, `SERVER El servidor se esta apagando...`.

## Decisiones de diseño

- **Streams**: `BufferedReader(InputStreamReader(..., UTF_8))` y `PrintWriter(OutputStreamWriter(..., UTF_8), true)` (autoFlush).
- **Concurrencia**: pool fijo (`newFixedThreadPool`), nunca `new Thread()` por cliente.
- **Thread-safety**: `ConcurrentHashMap` (salas y usuarios) + `CopyOnWriteArrayList` (miembros); sin `synchronized` alrededor de I/O. El nick se reserva de forma atómica con `putIfAbsent`.
- **Resiliencia (Ej. 5)**: `setSoTimeout(30000)`, `setKeepAlive(true)`; se capturan `SocketTimeoutException` y `SocketException`; el `finally` de `run()` siempre saca al usuario de la sala y libera el nick.
- **Shutdown hook**: cierra el `ServerSocket`, avisa a todos los clientes, cierra sus sockets y hace `shutdown()` / `awaitTermination()` del pool.
- **Detalle importante**: el aviso `TIMEOUT` se envía dentro del try-with-resources; si se hiciera en el `catch` externo, el socket ya estaría cerrado y el aviso se perdería.
- Los clientes inactivos 30 s son desconectados (no hay comando PING en el protocolo de la guía).

## Pruebas realizadas (JDK 21, con netcat y un script Python)

- Ej. 1: cliente/servidor de eco, cierre con `CHAO`.
- Ej. 2: dos clientes simultáneos atendidos por hilos distintos del pool.
- Ej. 3: comandos válidos e inválidos, orden CONNECT → JOIN → MSG, nick duplicado, línea vacía.
- Ej. 4: broadcast solo dentro de la sala, sin eco al emisor, salas aisladas.
- Ej. 5: timeout, caída abrupta (`Connection reset`) con liberación de sala y nick, shutdown hook con clientes conectados.
- Estrés: 20 clientes × 25 mensajes en la misma sala → cada uno recibió exactamente los 475 esperados, sin excepciones.

## Preguntas de autoevaluación (Módulos 1 y 2)

1. **`readLine()` sin `\n` recibido**: el hilo se bloquea esperando hasta que llegue un `\n`, se cierre la conexión (devuelve `null`) o venza el `setSoTimeout` (lanza `SocketTimeoutException`). Por eso todo mensaje debe terminar en salto de línea.
2. **Por qué `flush()` / autoFlush**: `PrintWriter` acumula en un búfer en memoria; sin vaciarlo los bytes no salen al socket y el otro extremo queda esperando eternamente.
3. **Dos clientes al mismo puerto 8080**: el S.O. identifica cada conexión TCP por la cuaterna (IP origen, puerto origen, IP destino, puerto destino). Cada cliente tiene un puerto efímero distinto, y `accept()` devuelve un `Socket` independiente por conexión.

## Lista de verificación

- [x] Cliente CLI con recepción asíncrona (hilo receptor + hilo principal leyendo `System.in`)
- [x] I/O dentro de try-with-resources / finally
- [x] Sin condiciones de carrera al unirse o salir de salas
- [x] Protocolo no ambiguo: cada mensaje termina en `\n`
