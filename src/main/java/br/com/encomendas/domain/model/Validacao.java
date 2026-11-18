package br.com.encomendas.domain.model;

import br.com.encomendas.domain.exception.RegraDeNegocioException;

/** Verificações reutilizadas pelas entidades do domínio. */
final class Validacao {

    private Validacao() {
    }

    static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new RegraDeNegocioException(mensagem);
        }
    }

    static <T> T exigirNaoNulo(T valor, String campo) {
        exigir(valor != null, "O campo '" + campo + "' é obrigatório");
        return valor;
    }

    static String exigirTexto(String valor, String campo) {
        exigir(valor != null && !valor.isBlank(), "O campo '" + campo + "' é obrigatório");
        return valor.trim();
    }

    static String textoOpcional(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
