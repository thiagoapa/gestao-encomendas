package br.com.encomendas.shared.util;

import br.com.encomendas.shared.exception.RegraNegocioException;

/** Verificações reutilizadas pelas entidades do domínio. */
public final class Validacao {

    private Validacao() {
    }

    public static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new RegraNegocioException(mensagem);
        }
    }

    public static <T> T exigirNaoNulo(T valor, String campo) {
        exigir(valor != null, "O campo '" + campo + "' é obrigatório");
        return valor;
    }

    public static String exigirTexto(String valor, String campo) {
        exigir(valor != null && !valor.isBlank(), "O campo '" + campo + "' é obrigatório");
        return valor.trim();
    }

    public static String textoOpcional(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
