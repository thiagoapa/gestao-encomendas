package br.com.encomendas.internal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Testa os endpoints de produto e funcionário de ponta a ponta (HTTP -> service -> banco H2). */
@SpringBootTest
@AutoConfigureMockMvc
class EndpointsProdutoFuncionarioTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void cadastraConsultaEReajustaProduto() throws Exception {
        String corpo = """
                {"nome": "Caneca personalizada", "descricao": "Porcelana 325 ml",
                 "precoBase": 40.00, "pontosCapacidade": 2}
                """;
        String resposta = mvc.perform(post("/produtos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ativo").value(true))
                .andReturn().getResponse().getContentAsString();
        String id = resposta.replaceAll(".*\"id\":(\\d+).*", "$1");

        // nome repetido -> regra de negócio (422)
        mvc.perform(post("/produtos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnprocessableEntity());

        mvc.perform(patch("/produtos/" + id + "/preco").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novoPreco\": 45.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precoBase").value(45.00));

        mvc.perform(get("/produtos")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nome").value("Caneca personalizada"));
    }

    @Test
    void validaCamposEInexistentes() throws Exception {
        mvc.perform(post("/produtos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"\", \"precoBase\": -1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").exists());

        mvc.perform(get("/funcionarios/999")).andExpect(status().isNotFound());
    }

    @Test
    void cadastraTransfereEDesativaFuncionario() throws Exception {
        String resposta = mvc.perform(post("/funcionarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Bruno\", \"setor\": \"ATENDIMENTO\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = resposta.replaceAll(".*\"id\":(\\d+).*", "$1");

        mvc.perform(patch("/funcionarios/" + id + "/setor").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novoSetor\": \"PRODUCAO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setor").value("PRODUCAO"));

        mvc.perform(get("/funcionarios").param("setor", "PRODUCAO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nome").value("Bruno"));

        mvc.perform(patch("/funcionarios/" + id + "/desativar")).andExpect(status().isNoContent());
        // desativar de novo -> regra de negócio (422)
        mvc.perform(patch("/funcionarios/" + id + "/desativar")).andExpect(status().isUnprocessableEntity());
    }
}
