import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Ejercicio 4 - Gestor central de salas y usuarios. Thread-safe sin synchronized extensos:
 *  - ConcurrentHashMap: NombreSala -> Lista de manejadores
 *  - CopyOnWriteArrayList: lecturas (broadcast) >> escrituras (join/leave)
 */
public class RoomManager {

    private final Map<String, List<ClientHandler>> salas = new ConcurrentHashMap<>();
    private final Map<String, ClientHandler> usuarios = new ConcurrentHashMap<>();

    // ---- Usuarios ----
    /** @return true si el nick estaba libre (operacion atomica). */
    public boolean registrarUsuario(String nick, ClientHandler h) {
        return usuarios.putIfAbsent(nick, h) == null;
    }

    public void liberarUsuario(String nick, ClientHandler h) {
        if (nick != null) usuarios.remove(nick, h); // solo si sigue siendo el mismo handler
    }

    // ---- Salas ----
    public void unirseASala(String nombreSala, ClientHandler cliente) {
        salas.computeIfAbsent(nombreSala, k -> new CopyOnWriteArrayList<>()).add(cliente);
    }

    public void salirDeSala(String nombreSala, ClientHandler cliente) {
        if (nombreSala == null) return;
        List<ClientHandler> miembros = salas.get(nombreSala);
        if (miembros != null) miembros.remove(cliente);
    }

    /** Envia el mensaje a todos los miembros de la sala menos al emisor. */
    public void broadcast(String nombreSala, String mensaje, ClientHandler emisor) {
        List<ClientHandler> miembros = salas.get(nombreSala);
        if (miembros != null) {
            for (ClientHandler miembro : miembros) {
                if (miembro != emisor) {
                    miembro.enviarMensajeDirecto(mensaje);
                }
            }
        }
    }

    /** Usado por el shutdown hook: avisa a todos los usuarios conectados. */
    public void notificarATodos(String mensaje) {
        for (ClientHandler h : usuarios.values()) {
            h.enviarMensajeDirecto(mensaje);
        }
    }

    public Collection<ClientHandler> usuariosConectados() {
        return new ArrayList<>(usuarios.values());
    }

    public List<String> miembrosDe(String sala) {
        List<String> nombres = new ArrayList<>();
        List<ClientHandler> miembros = salas.get(sala);
        if (miembros != null) for (ClientHandler h : miembros) nombres.add(h.getUsername());
        return nombres;
    }
}
