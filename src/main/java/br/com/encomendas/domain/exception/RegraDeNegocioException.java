package br.com.encomendas.domain.exception;

/**
 * Lançada quando uma operação viola uma regra do negócio
 * (ex.: entregar uma encomenda com saldo pendente).
 */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
