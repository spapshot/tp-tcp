import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/** Ejercicio 1 - Cliente: lee de System.in, envía al servidor y muestra la respuesta. */
public class EchoClient {
    public static void main(String[] args) {
        String host = "localhost";
        int puerto = 8080;

        try (Socket socket = new Socket(host, puerto);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader consola = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Conectado a " + host + ":" + puerto + ". Escribí CHAO para salir.");
            String linea;
            while ((linea = consola.readLine()) != null) {
                out.println(linea);                        // println + autoFlush => termina en \n
                if ("CHAO".equalsIgnoreCase(linea)) break; // el servidor cierra al recibir CHAO
                String respuesta = in.readLine();
                if (respuesta == null) {
                    System.out.println("El servidor cerró la conexión.");
                    break;
                }
                System.out.println(respuesta);
            }
        } catch (ConnectException e) {
            System.err.println("No se pudo conectar: ¿está corriendo el servidor? " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error de E/S: " + e.getMessage());
        }
    }
}
