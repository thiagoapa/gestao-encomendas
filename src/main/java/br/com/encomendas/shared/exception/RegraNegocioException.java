package br.com.encomendas.shared.exception;

/**
 * Lançada quando uma operação viola uma regra do negócio
 * (ex.: entregar uma encomenda com saldo pendente).
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
