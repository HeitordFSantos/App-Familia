package com.appfamilia;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.appfamilia.devocional.biblia.FonteBiblica;
import com.appfamilia.shared.ia.ClienteLlm;
import com.appfamilia.shared.ia.Pesquisa;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Devocional diário e leitura de fotos, com o modelo de linguagem, a Bíblia e a pesquisa simulados. */
@SpringBootTest
@AutoConfigureMockMvc
class IaIntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @MockitoBean
    ClienteLlm llm;

    @MockitoBean
    FonteBiblica biblia;

    @MockitoBean
    Pesquisa pesquisa;

    @Test
    void devocionalPorLivro() throws Exception {
        String token = novaCasa("dev-livro");
        when(llm.configurado()).thenReturn(true);
        when(biblia.buscar(any())).thenReturn(Optional.of(new FonteBiblica.Texto("[1] Texto do salmo", "© Detentor")));
        when(pesquisa.buscar(anyString(), anyInt())).thenReturn(List.of(
                new Pesquisa.Resultado("Estudo do Salmo 1", "https://exemplo.org/salmo-1", "Um resumo")));
        when(llm.conversar(anyString(), anyString())).thenReturn("""
                Aqui está:
                ```json
                {"titulo":"Dois caminhos","reflexao":"Texto da reflexão.","aplicacao":"Ler juntos.","oracao":"Senhor, guia-nos."}
                ```""");

        // Sem plano escolhido, começa pelos Salmos, capítulo 1
        enviar(get("/api/devocional/hoje"), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referencia").value("Salmos 1"))
                .andExpect(jsonPath("$.textoBiblico").value("[1] Texto do salmo"))
                .andExpect(jsonPath("$.direitos").value("© Detentor"))
                .andExpect(jsonPath("$.titulo").value("Dois caminhos"))
                .andExpect(jsonPath("$.oracao").value("Senhor, guia-nos."))
                .andExpect(jsonPath("$.fontes[0].url").value("https://exemplo.org/salmo-1"));

        // No mesmo dia o devocional não é gerado de novo, e o plano já aponta para o capítulo seguinte
        enviar(get("/api/devocional/hoje"), token, null).andExpect(jsonPath("$.titulo").value("Dois caminhos"));
        verify(llm, times(1)).conversar(anyString(), anyString());
        enviar(get("/api/devocional/plano"), token, null)
                .andExpect(jsonPath("$.tipo").value("LIVRO"))
                .andExpect(jsonPath("$.proximoCapitulo").value(2));
    }

    @Test
    void devocionalPorTema() throws Exception {
        String token = novaCasa("dev-tema");
        when(llm.configurado()).thenReturn(true);
        when(biblia.buscar(any())).thenReturn(Optional.empty());
        when(pesquisa.buscar(anyString(), anyInt())).thenReturn(List.of());

        enviar(put("/api/devocional/plano"), token, """
                {"tipo":"LIVRO","valor":"XYZ"}""").andExpect(status().isBadRequest());
        enviar(put("/api/devocional/plano"), token, """
                {"tipo":"TEMA","valor":"gratidão"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Tema: gratidão"));

        // Primeira resposta escolhe a passagem; a segunda, fora do formato, vira a reflexão inteira
        when(llm.conversar(anyString(), anyString()))
                .thenReturn("{\"livro\":\"1TH\",\"capitulo\":5,\"versiculoInicio\":16,\"versiculoFim\":18}")
                .thenReturn("Um texto corrido, sem JSON.");

        enviar(get("/api/devocional/hoje"), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referencia").value("1 Tessalonicenses 5:16-18"))
                .andExpect(jsonPath("$.textoBiblico").doesNotExist())
                .andExpect(jsonPath("$.reflexao").value("Um texto corrido, sem JSON."))
                .andExpect(jsonPath("$.fontes.length()").value(0));
    }

    @Test
    void devocionalSemModeloConfigurado() throws Exception {
        String token = novaCasa("dev-sem-llm");
        when(llm.configurado()).thenReturn(false);
        enviar(get("/api/devocional/hoje"), token, null).andExpect(status().isServiceUnavailable());
        enviar(get("/api/devocional/livros"), token, null).andExpect(jsonPath("$.length()").value(66));
    }

    @Test
    void cadernetaPorFoto() throws Exception {
        String token = novaCasa("caderneta");
        String nascimento = LocalDate.now().minusMonths(6).toString();
        String filho = corpo(enviar(post("/api/dependentes"), token, """
                {"nome":"Davi","dataNascimento":"%s"}""".formatted(nascimento))).get("id").asText();
        String caminho = "/api/dependentes/" + filho + "/vacinas";
        enviar(put(caminho + "/BCG_UNICA"), token, """
                {"aplicadaEm":"%s"}""".formatted(nascimento)).andExpect(status().isNoContent());

        // O modelo devolve duas doses válidas, um código inventado, uma data futura e uma repetida
        when(llm.lerImagem(anyString(), anyString(), any())).thenReturn("""
                {"doses":[
                  {"codigo":"BCG_UNICA","data":"%s"},
                  {"codigo":"PENTA_1","data":"%s"},
                  {"codigo":"INVENTADA","data":"%s"},
                  {"codigo":"VIP_1","data":"2999-01-01"},
                  {"codigo":"PENTA_1","data":"%s"}
                ]}""".formatted(nascimento, hojeMenos(60), nascimento, hojeMenos(30)));

        enviar(post(caminho + "/foto"), token, """
                {"imagemBase64":"QUJD","tipo":"image/jpeg"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].codigo").value("BCG_UNICA"))
                .andExpect(jsonPath("$[0].jaMarcada").value(true))
                .andExpect(jsonPath("$[1].codigo").value("PENTA_1"))
                .andExpect(jsonPath("$[1].aplicadaEm").value(hojeMenos(60)))
                .andExpect(jsonPath("$[1].jaMarcada").value(false));

        // A leitura só sugere: nada foi gravado além do que já estava marcado
        JsonNode agenda = corpo(enviar(get(caminho), token, null));
        for (JsonNode dose : agenda) {
            if (dose.get("codigo").asText().equals("PENTA_1")) {
                org.junit.jupiter.api.Assertions.assertNotEquals("APLICADA", dose.get("situacao").asText());
            }
        }
    }

    private static String hojeMenos(int dias) {
        return LocalDate.now().minusDays(dias).toString();
    }

    private String novaCasa(String apelido) throws Exception {
        return corpo(enviar(post("/api/auth/cadastro"), null, """
                {"nome":"Teste","email":"%s@teste.com","senha":"senha-forte-1","nomeCasa":"Casa %s"}"""
                .formatted(apelido, apelido)).andExpect(status().isCreated())).get("token").asText();
    }

    private ResultActions enviar(MockHttpServletRequestBuilder requisicao, String token, String corpo) throws Exception {
        if (token != null) {
            requisicao.header("Authorization", "Bearer " + token);
        }
        if (corpo != null) {
            requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo);
        }
        return mvc.perform(requisicao);
    }

    private JsonNode corpo(ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsString());
    }
}
