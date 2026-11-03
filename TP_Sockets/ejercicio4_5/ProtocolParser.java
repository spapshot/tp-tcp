/**
 * Ejercicio 3 - Analiza una línea de texto de la red y devuelve un Command.
 * Nunca lanza excepciones: ante sintaxis inválida devuelve Command.invalido(descripcion).
 */
public class ProtocolParser {

    public static final int MAX_LONGITUD_LINEA = 1000;

    public static Command parse(String lineaBruta) {
        if (lineaBruta == null || lineaBruta.trim().isEmpty()) {
            return Command.invalido("Linea vacia");
        }
        if (lineaBruta.length() > MAX_LONGITUD_LINEA) {
            return Command.invalido("Linea demasiado larga (maximo " + MAX_LONGITUD_LINEA + " caracteres)");
        }

        String[] partes = lineaBruta.trim().split("\\s+", 2);
        String comando = partes[0].toUpperCase();
        String argumento = partes.length > 1 ? partes[1].trim() : "";

        switch (comando) {
            case "CONNECT":
                if (argumento.isEmpty())
                    return Command.invalido("Sintaxis: CONNECT <username>");
                if (!argumento.matches("[A-Za-z0-9_]{1,20}"))
                    return Command.invalido("Username invalido: solo letras, numeros y _ (1 a 20 caracteres)");
                return Command.valido(Command.Tipo.CONNECT, argumento);

            case "JOIN":
                if (argumento.isEmpty())
                    return Command.invalido("Sintaxis: JOIN <nombre_sala>");
                if (!argumento.matches("#?[A-Za-z0-9_]{1,30}"))
                    return Command.invalido("Nombre de sala invalido: solo letras, numeros y _ (opcional # inicial)");
                return Command.valido(Command.Tipo.JOIN, argumento);

            case "MSG":
                if (argumento.isEmpty())
                    return Command.invalido("Sintaxis: MSG <texto>");
                return Command.valido(Command.Tipo.MSG, argumento);

            case "QUIT":
                if (!argumento.isEmpty())
                    return Command.invalido("Sintaxis: QUIT (sin argumentos)");
                return Command.valido(Command.Tipo.QUIT, "");

            default:
                return Command.invalido("Comando no reconocido: " + comando);
        }
    }
}
