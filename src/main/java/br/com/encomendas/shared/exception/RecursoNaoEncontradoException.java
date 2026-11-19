package br.com.encomendas.shared.exception;

/** Lançada quando um registro buscado pelo identificador não existe (vira HTTP 404). */
public class RecursoNaoEncontradoException extends RuntimeException {

    private final String recurso;
    private final Object identificador;

    public RecursoNaoEncontradoException(String recurso, Object identificador) {
        super(recurso + " não encontrado para o identificador: " + identificador);
        this.recurso = recurso;
        this.identificador = identificador;
    }

    public String getRecurso() { return recurso; }
    public Object getIdentificador() { return identificador; }
}
