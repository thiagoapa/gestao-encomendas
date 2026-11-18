package br.com.encomendas.internal.encomenda.entity;

/** Categoria de cada registro da trilha de auditoria da encomenda. */
public enum TipoAlteracao {
    CRIACAO,
    ITENS,
    ESPECIFICACAO,
    PRAZO,
    STATUS,
    PAGAMENTO,
    EQUIPE,
    ANEXO,
    CONDICAO_FINANCEIRA
}
