import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * Cliente CLI con recepcion asincrona: un hilo dedicado escucha al servidor
 * mientras el hilo principal lee de System.in.
 * Uso: java JsmsClient [host] [puerto]
 */
public class JsmsClient {
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int puerto = args.length > 1 ? Integer.parseInt(args[1]) : 8080;

        try (Socket socket = new Socket(host, puerto);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader consola = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Conectado a " + host + ":" + puerto);
            System.out.println("Comandos: CONNECT <user> | JOIN <sala> | MSG <texto> | QUIT");

            // Hilo receptor (asincrono)
            Thread receptor = new Thread(() -> {
                try {
                    String linea;
                    while ((linea = in.readLine()) != null) {
                        System.out.println("<< " + linea);
                    }
                    System.out.println("<< [conexion cerrada por el servidor]");
                } catch (IOException e) {
                    if (!socket.isClosed()) System.out.println("<< [conexion perdida: " + e.getMessage() + "]");
                }
            }, "receptor");
            receptor.setDaemon(true);
            receptor.start();

            // Hilo principal: teclado -> servidor
            String linea;
            while ((linea = consola.readLine()) != null) {
                out.println(linea);
                if ("QUIT".equalsIgnoreCase(linea.trim())) break;
                if (!receptor.isAlive()) break;
            }
            try { receptor.join(500); } catch (InterruptedException ignored) { }
        } catch (ConnectException e) {
            System.err.println("No se pudo conectar: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error de E/S: " + e.getMessage());
        }
    }
}
