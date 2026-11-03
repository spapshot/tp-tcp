import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/** Ejercicio 1 - Servidor de eco mono-hilo. */
public class EchoServer {
    public static void main(String[] args) throws IOException {
        int puerto = 8080;
        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("Servidor iniciado en puerto " + puerto);
            Socket clientSocket = serverSocket.accept(); // Bloqueante
            System.out.println("Cliente conectado desde: " + clientSocket.getInetAddress());

            try (BufferedReader in = new BufferedReader(
                         new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter out = new PrintWriter(
                         new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true)) {

                String mensaje;
                while ((mensaje = in.readLine()) != null) {
                    if ("CHAO".equalsIgnoreCase(mensaje)) break;
                    out.println("ECO: " + mensaje);
                }
            } finally {
                clientSocket.close();
            }
        }
    }
}
