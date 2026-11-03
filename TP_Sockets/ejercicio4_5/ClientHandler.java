import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * Ejercicios 4 y 5 - Manejador completo de un cliente JSMS.
 * Protocolo (cada mensaje termina en \n):
 *   CONNECT <user> | JOIN <sala> | MSG <texto> | QUIT
 */
public class ClientHandler implements Runnable {

    /** Timeout de lectura (Ej. 5). Configurable con -Djsms.timeout=ms para pruebas. */
    public static final int TIMEOUT_MS = Integer.getInteger("jsms.timeout", 30000);

    private final Socket socket;
    private final RoomManager rooms;
    private volatile PrintWriter out;
    private volatile String username; // null hasta CONNECT
    private volatile String sala;     // null hasta JOIN

    public ClientHandler(Socket socket, RoomManager rooms) {
        this.socket = socket;
        this.rooms = rooms;
    }

    public String getUsername() { return username; }

    /** Envio thread-safe: PrintWriter.println sincroniza internamente; no hay I/O bajo synchronized propio. */
    public void enviarMensajeDirecto(String mensaje) {
        PrintWriter w = out;
        if (w != null) w.println(mensaje);
    }

    /** Cierra el socket (lo usa el shutdown hook para desbloquear readLine()). */
    public void cerrarSocket() {
        try { socket.close(); } catch (IOException ignored) { }
    }

    @Override
    public void run() {
        try (Socket s = socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter w = new PrintWriter(
                     new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true)) {

            this.out = w;
            s.setSoTimeout(TIMEOUT_MS);   // detecta clientes inactivos / caidas de red
            s.setKeepAlive(true);         // keep-alive TCP a nivel de S.O.

            try {
                String linea;
                while ((linea = in.readLine()) != null) {   // null => EOF (cliente cerro)
                    Command cmd = ProtocolParser.parse(linea);
                    if (!procesar(cmd)) break;
                }
            } catch (SocketTimeoutException e) {
                // Se atrapa DENTRO del try-with-resources: si no, el socket ya estaria cerrado
                // cuando se ejecuta el catch y el aviso nunca llegaria al cliente.
                System.err.println("Inactividad de " + descripcion() + ". Cerrando por timeout.");
                enviarMensajeDirecto("TIMEOUT Conexion cerrada por inactividad");
            }
        } catch (SocketException e) {
            System.err.println(descripcion() + " cerro la conexion abruptamente: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error de E/S con " + descripcion() + ": " + e.getMessage());
        } finally {
            limpiar();
        }
    }

    /** @return false si hay que cerrar la sesion. */
    private boolean procesar(Command cmd) {
        switch (cmd.getTipo()) {
            case CONNECT:
                if (username != null) {
                    enviarMensajeDirecto("401 ERROR Ya estas conectado como " + username);
                } else if (rooms.registrarUsuario(cmd.getArgumento(), this)) {
                    username = cmd.getArgumento();
                    enviarMensajeDirecto("200 OK");
                } else {
                    enviarMensajeDirecto("401 ERROR El usuario '" + cmd.getArgumento() + "' ya existe");
                }
                break;

            case JOIN:
                if (!requiereConnect()) break;
                String nueva = cmd.getArgumento();
                String anterior = sala;
                if (nueva.equals(anterior)) {
                    enviarMensajeDirecto("ERROR Ya estas en la sala " + nueva);
                    break;
                }
                if (anterior != null) {
                    rooms.salirDeSala(anterior, this);
                    rooms.broadcast(anterior, "INFO " + username + " salio de " + anterior, this);
                }
                rooms.unirseASala(nueva, this);
                sala = nueva;
                enviarMensajeDirecto("JOINED " + nueva);
                rooms.broadcast(nueva, "INFO " + username + " se unio a " + nueva, this);
                break;

            case MSG:
                if (!requiereConnect()) break;
                if (sala == null) { enviarMensajeDirecto("ERROR Primero debes hacer JOIN <sala>"); break; }
                rooms.broadcast(sala, "MSG " + username + ": " + cmd.getArgumento(), this);
                enviarMensajeDirecto("200 OK");  // acuse al emisor (no recibe eco de su propio mensaje)
                break;

            case QUIT:
                enviarMensajeDirecto("BYE");
                return false;

            default: // INVALIDO: error descriptivo SIN tumbar el socket
                enviarMensajeDirecto("ERROR " + cmd.getError());
        }
        return true;
    }

    private boolean requiereConnect() {
        if (username == null) {
            enviarMensajeDirecto("ERROR Primero debes hacer CONNECT <username>");
            return false;
        }
        return true;
    }

    /** Libera recursos de salas/usuarios; se ejecuta SIEMPRE (QUIT, EOF, timeout, error, shutdown). */
    private void limpiar() {
        String s = sala;
        String u = username;
        if (s != null) {
            rooms.salirDeSala(s, this);
            if (u != null) rooms.broadcast(s, "INFO " + u + " se desconecto", this);
        }
        rooms.liberarUsuario(u, this);
        System.out.println("Sesion finalizada: " + descripcion());
    }

    private String descripcion() {
        return (username != null ? username : "anonimo") + "@" + socket.getRemoteSocketAddress();
    }
}
