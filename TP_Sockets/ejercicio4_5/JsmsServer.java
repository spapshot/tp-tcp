import java.io.IOException;
import java.net.*;
import java.util.concurrent.*;

/** Ejercicios 2, 4 y 5 integrados - Servidor JSMS con pool de hilos y apagado limpio. */
public class JsmsServer {

    private static final int PUERTO = Integer.getInteger("jsms.port", 8080);
    private static final int MAX_HILOS = 50;

    private final ExecutorService threadPool = Executors.newFixedThreadPool(MAX_HILOS);
    private final RoomManager rooms = new RoomManager();
    private volatile ServerSocket serverSocket;

    public void iniciar(int puerto) throws IOException {
        serverSocket = new ServerSocket(puerto);
        registrarShutdownHook();
        System.out.println("JSMS escuchando en el puerto " + puerto + " (timeout " + ClientHandler.TIMEOUT_MS + " ms)");

        try {
            while (!serverSocket.isClosed()) {
                Socket cliente = serverSocket.accept();
                threadPool.execute(new ClientHandler(cliente, rooms));
            }
        } catch (SocketException e) {
            // accept() interrumpido porque el hook cerro el ServerSocket: apagado normal
            System.out.println("ServerSocket cerrado, dejando de aceptar conexiones.");
        } finally {
            threadPool.shutdown();
        }
    }

    /** Ejercicio 5: notifica a los clientes antes de apagar y libera puerto y hilos. */
    private void registrarShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nIniciando apagado controlado del servidor...");
            try { serverSocket.close(); } catch (IOException ignored) { }   // no aceptar mas clientes

            rooms.notificarATodos("SERVER El servidor se esta apagando. Hasta pronto.");
            for (ClientHandler h : rooms.usuariosConectados()) h.cerrarSocket(); // desbloquea readLine()

            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(3, TimeUnit.SECONDS)) threadPool.shutdownNow();
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
            System.out.println("Servidor detenido.");
        }, "shutdown-hook"));
    }

    public static void main(String[] args) throws IOException {
        new JsmsServer().iniciar(PUERTO);
    }
}
