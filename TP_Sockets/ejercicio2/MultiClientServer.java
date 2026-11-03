import java.io.IOException;
import java.net.*;
import java.util.concurrent.*;

/** Ejercicio 2 - Servidor multihilo con thread pool fijo. */
public class MultiClientServer {
    private static final int PUERTO = 8080;
    private static final ExecutorService pool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws IOException {
        try (ServerSocket server = new ServerSocket(PUERTO)) {
            System.out.println("Servidor Multihilo listo en puerto " + PUERTO + "...");
            while (true) {
                Socket socket = server.accept();
                pool.execute(new ClientHandler(socket)); // asigna un hilo del pool
            }
        } finally {
            pool.shutdown();
        }
    }
}
