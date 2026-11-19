# Planejamento — Etapa 3 (Desenvolvimento Colaborativo)

## 1. O que é mais relevante para a primeira versão?

O centro do sistema é a **encomenda** (status, prazo, pagamentos e ficha de produção): é ali que estão os prejuízos da empresa — retrabalho, atrasos e entregas sem receber.

## 2. O que depende do quê?

Uma encomenda não existe sozinha: ela precisa de um **cliente**, de **produtos** do catálogo e de um **funcionário** responsável por cada ação.

```
cliente ─────┐
produto ─────┼──▶ encomenda (cadastro, itens, prazo) ──▶ pagamentos ──▶ consulta de pedidos
funcionario ─┘
```

Por isso a ordem de implementação é:

| Rodada | Funcionalidade | Por quê |
|---|---|---|
| 1 | Cadastros base: clientes, produtos, funcionários | Sem eles não dá para abrir uma encomenda |
| 2 | Cadastro de encomendas (itens, prazo com capacidade, confirmação) | Resolve o problema prioritário da Etapa 1 |
| 3 | Controle de pagamentos (sinal, saldo, bloqueio da entrega) | Depende da encomenda confirmada |
| 4 | Consulta de pedidos (status e saldo para o atendente) | Depende de encomendas e pagamentos |

## 3. Divisão das responsabilidades (rodada 1)

| Integrante | Branch | Entrega |
|---|---|---|
| Tales Vitor | `feature/cadastro-cliente` | Endpoints de clientes |
| Thiago | `feature/cadastro-produtos-funcionarios` | Endpoints de produtos e funcionários + tratamento global de erros |

Rodada 2 em diante: um integrante fica com **cadastro de encomendas** e o outro com **controle de pagamentos**; a **consulta de pedidos** é feita em conjunto.

## 4. Fluxo de trabalho no Git

1. `git checkout main && git pull` — atualizar a main local
2. `git checkout -b feature/<funcionalidade>` — criar a branch da funcionalidade
3. Commits pequenos com mensagens claras (`feat(produto): ...`, `fix: ...`, `docs: ...`)
4. `git push -u origin feature/<funcionalidade>`
5. Abrir Pull Request para a `main`; o outro integrante revisa antes do merge

## 5. Padrões combinados

- Cada módulo em `internal/<modulo>/` com `controller`, `dto`, `mapper`, `repository`, `service` e `entity`
- Controllers nunca recebem nem devolvem entidades: usam DTOs (`record`) e o `Mapper` do módulo
- Regras de negócio ficam nas entidades; o service só busca, chama o método do domínio e salva
- Erros: `RecursoNaoEncontradoException` → 404, `RegraNegocioException` → 422, validação → 400 (`config/exception/GlobalExceptionHandler`)
