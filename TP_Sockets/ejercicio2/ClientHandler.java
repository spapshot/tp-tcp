import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/** Ejercicio 2 - Manejador de eco ejecutado por un hilo del pool. */
public class ClientHandler implements Runnable {
    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        String hilo = Thread.currentThread().getName();
        System.out.println("[" + hilo + "] atendiendo a " + socket.getRemoteSocketAddress());
        try (Socket s = socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true)) {

            String mensaje;
            while ((mensaje = in.readLine()) != null) {
                if ("CHAO".equalsIgnoreCase(mensaje.trim())) break;
                out.println("ECO [" + hilo + "]: " + mensaje);
            }
        } catch (IOException e) {
            System.err.println("[" + hilo + "] cliente desconectado: " + e.getMessage());
        }
        System.out.println("[" + hilo + "] conexión finalizada");
    }
}
