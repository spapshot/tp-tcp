import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ejercicio 3 - Manejador que implementa el protocolo (CONNECT, JOIN, MSG, QUIT)
 * SIN broadcast todavia: MSG solo responde al propio usuario "MSG <user>: <texto>".
 */
public class ClientHandler implements Runnable {

    // Usernames unicos en todo el servidor (conjunto thread-safe)
    private static final Set<String> USUARIOS = ConcurrentHashMap.newKeySet();

    private final Socket socket;
    private PrintWriter out;
    private String username;  // null hasta CONNECT
    private String sala;      // null hasta JOIN

    public ClientHandler(Socket socket) { this.socket = socket; }

    @Override
    public void run() {
        try (Socket s = socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter w = new PrintWriter(
                     new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true)) {

            this.out = w;
            String linea;
            while ((linea = in.readLine()) != null) {
                Command cmd = ProtocolParser.parse(linea);
                if (!procesar(cmd)) break;
            }
        } catch (IOException e) {
            System.err.println("Cliente desconectado: " + e.getMessage());
        } finally {
            if (username != null) USUARIOS.remove(username);
        }
    }

    /** @return false si hay que cerrar la sesion. */
    private boolean procesar(Command cmd) {
        switch (cmd.getTipo()) {
            case CONNECT:
                if (username != null) { out.println("401 ERROR Ya estas conectado como " + username); break; }
                if (USUARIOS.add(cmd.getArgumento())) {
                    username = cmd.getArgumento();
                    out.println("200 OK");
                } else {
                    out.println("401 ERROR El usuario '" + cmd.getArgumento() + "' ya existe");
                }
                break;
            case JOIN:
                if (username == null) { out.println("ERROR Primero debes hacer CONNECT <username>"); break; }
                sala = cmd.getArgumento();
                out.println("JOINED " + sala);
                break;
            case MSG:
                if (username == null) { out.println("ERROR Primero debes hacer CONNECT <username>"); break; }
                if (sala == null)     { out.println("ERROR Primero debes hacer JOIN <sala>"); break; }
                out.println("MSG " + username + ": " + cmd.getArgumento());
                break;
            case QUIT:
                out.println("BYE");
                return false;
            default: // INVALIDO: responde error descriptivo SIN cerrar el socket
                out.println("ERROR " + cmd.getError());
        }
        return true;
    }
}
