package br.com.encomendas.domain.enums;

/**
 * Ciclo de vida da encomenda (definido na Etapa 1) e as transições permitidas.
 *
 * <pre>
 * RASCUNHO -> PENDENTE -> EM_PRODUCAO -> PRONTO -> ENTREGUE
 *                                ^__________|  (alteração após pronto = retrabalho)
 * Qualquer estado, exceto ENTREGUE, pode ir para CANCELADA.
 * </pre>
 */
public enum StatusEncomenda {
    RASCUNHO,
    PENDENTE,
    EM_PRODUCAO,
    PRONTO,
    ENTREGUE,
    CANCELADA;

    public boolean podeTransitarPara(StatusEncomenda destino) {
        return switch (this) {
            case RASCUNHO -> destino == PENDENTE || destino == CANCELADA;
            case PENDENTE -> destino == EM_PRODUCAO || destino == CANCELADA;
            case EM_PRODUCAO -> destino == PRONTO || destino == CANCELADA;
            case PRONTO -> destino == ENTREGUE || destino == EM_PRODUCAO || destino == CANCELADA;
            case ENTREGUE, CANCELADA -> false;
        };
    }
}
