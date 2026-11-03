/** Ejercicio 3 - Representación estructurada de un comando del protocolo JSMS. */
public class Command {
    public enum Tipo { CONNECT, JOIN, MSG, QUIT, INVALIDO }

    private final Tipo tipo;
    private final String argumento;
    private final String error; // solo si tipo == INVALIDO

    private Command(Tipo tipo, String argumento, String error) {
        this.tipo = tipo;
        this.argumento = argumento;
        this.error = error;
    }

    public static Command valido(Tipo tipo, String argumento) { return new Command(tipo, argumento, null); }
    public static Command invalido(String error)              { return new Command(Tipo.INVALIDO, "", error); }

    public Tipo getTipo()        { return tipo; }
    public String getArgumento() { return argumento; }
    public String getError()     { return error; }

    @Override
    public String toString() { return tipo + (argumento.isEmpty() ? "" : "(" + argumento + ")"); }
}
