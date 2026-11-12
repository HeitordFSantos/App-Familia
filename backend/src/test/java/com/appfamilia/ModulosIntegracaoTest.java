package com.appfamilia;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.appfamilia.vacinas.CalendarioVacinal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Agenda, vacinas, compras e finanças, incluindo o isolamento entre Casas. */
@SpringBootTest
@AutoConfigureMockMvc
class ModulosIntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Test
    void agenda() throws Exception {
        String token = novaCasa("agenda");
        String outra = novaCasa("agenda-outra");
        String filho = corpo(enviar(post("/api/dependentes"), token, """
                {"nome":"Bia","dataNascimento":"2024-03-10"}""")).get("id").asText();
        String amanha = LocalDate.now().plusDays(1) + "T14:30:15";

        String id = corpo(enviar(post("/api/compromissos"), token, """
                {"titulo":"Pediatra","inicio":"%s","lembreteMinutos":60,"dependenteId":"%s"}""".formatted(amanha, filho))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inicio").value(amanha))).get("id").asText();
        enviar(post("/api/compromissos"), token, """
                {"titulo":"Antigo","inicio":"2020-01-01T10:00:00"}""").andExpect(status().isCreated());

        // Só aparecem os de hoje em diante
        enviar(get("/api/compromissos"), token, null)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Pediatra"));
        enviar(get("/api/compromissos").param("de", "2019-01-01"), token, null)
                .andExpect(jsonPath("$.length()").value(2));

        // Filho de outra Casa é recusado; outra Casa não vê nem apaga
        enviar(post("/api/compromissos"), outra, """
                {"titulo":"X","inicio":"%s","dependenteId":"%s"}""".formatted(amanha, filho))
                .andExpect(status().isBadRequest());
        enviar(get("/api/compromissos"), outra, null).andExpect(jsonPath("$.length()").value(0));
        enviar(delete("/api/compromissos/" + id), outra, null).andExpect(status().isNotFound());
        enviar(delete("/api/compromissos/" + id), token, null).andExpect(status().isNoContent());
    }

    @Test
    void vacinas() throws Exception {
        String token = novaCasa("vacinas");
        String outra = novaCasa("vacinas-outra");
        // Nasceu há pouco menos de 4 meses: a dose dos 4 meses cai nos próximos dias
        String nascimento = LocalDate.now().minusMonths(4).plusDays(10).toString();
        String filho = corpo(enviar(post("/api/dependentes"), token, """
                {"nome":"Caio","dataNascimento":"%s"}""".formatted(nascimento))).get("id").asText();
        String caminho = "/api/dependentes/" + filho + "/vacinas";

        // Os códigos do calendário não podem se repetir
        var codigos = new HashSet<String>();
        CalendarioVacinal.DOSES.forEach(d -> Assertions.assertTrue(codigos.add(d.codigo()), d.codigo()));

        JsonNode agenda = corpo(enviar(get(caminho), token, null).andExpect(status().isOk()));
        Assertions.assertEquals(CalendarioVacinal.DOSES.size(), agenda.size());
        Assertions.assertEquals("PENDENTE", situacao(agenda, "BCG_UNICA"));
        Assertions.assertEquals("PROXIMA", situacao(agenda, "PENTA_2"));
        Assertions.assertEquals("FUTURA", situacao(agenda, "HPV_UNICA"));

        enviar(put(caminho + "/BCG_UNICA"), token, """
                {"aplicadaEm":"%s"}""".formatted(nascimento)).andExpect(status().isNoContent());
        Assertions.assertEquals("APLICADA", situacao(corpo(enviar(get(caminho), token, null)), "BCG_UNICA"));

        enviar(put(caminho + "/NAO_EXISTE"), token, """
                {"aplicadaEm":"%s"}""".formatted(nascimento)).andExpect(status().isNotFound());
        enviar(get(caminho), outra, null).andExpect(status().isNotFound());

        enviar(delete(caminho + "/BCG_UNICA"), token, null).andExpect(status().isNoContent());
        Assertions.assertEquals("PENDENTE", situacao(corpo(enviar(get(caminho), token, null)), "BCG_UNICA"));

        // Remover o filho leva junto os registros de vacina
        enviar(put(caminho + "/HEPB_1"), token, """
                {"aplicadaEm":"%s"}""".formatted(nascimento)).andExpect(status().isNoContent());
        enviar(delete("/api/dependentes/" + filho), token, null).andExpect(status().isNoContent());
    }

    @Test
    void compras() throws Exception {
        String token = novaCasa("compras");
        String outra = novaCasa("compras-outra");

        String arroz = corpo(enviar(post("/api/compras"), token, """
                {"nome":"Arroz","quantidade":"2 kg"}""").andExpect(status().isCreated())).get("id").asText();
        enviar(post("/api/compras"), token, """
                {"nome":"Leite"}""").andExpect(status().isCreated());

        enviar(put("/api/compras/" + arroz + "/comprado"), token, """
                {"comprado":true}""").andExpect(jsonPath("$.comprado").value(true));
        // Comprados vão para o fim da lista
        enviar(get("/api/compras"), token, null)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Leite"))
                .andExpect(jsonPath("$[1].nome").value("Arroz"));

        enviar(get("/api/compras"), outra, null).andExpect(jsonPath("$.length()").value(0));
        enviar(delete("/api/compras/" + arroz), outra, null).andExpect(status().isNotFound());

        enviar(delete("/api/compras/comprados"), token, null).andExpect(status().isNoContent());
        enviar(get("/api/compras"), token, null)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Leite"));
    }

    @Test
    void financas() throws Exception {
        String token = novaCasa("financas");
        String outra = novaCasa("financas-outra");
        String hoje = LocalDate.now().toString();

        enviar(post("/api/lancamentos"), token, """
                {"tipo":"RECEITA","valorCentavos":500000,"categoria":"Salário","data":"%s"}""".formatted(hoje))
                .andExpect(status().isCreated());
        String mercado = corpo(enviar(post("/api/lancamentos"), token, """
                {"tipo":"DESPESA","valorCentavos":123456,"categoria":"Mercado","descricao":"Compra do mês","data":"%s"}"""
                .formatted(hoje))).get("id").asText();
        enviar(post("/api/lancamentos"), token, """
                {"tipo":"DESPESA","valorCentavos":9900,"categoria":"Antigo","data":"2020-01-15"}""")
                .andExpect(status().isCreated());
        enviar(post("/api/lancamentos"), token, """
                {"tipo":"DESPESA","valorCentavos":0,"categoria":"Zero","data":"%s"}""".formatted(hoje))
                .andExpect(status().isBadRequest());

        enviar(get("/api/lancamentos"), token, null)
                .andExpect(jsonPath("$.mes").value(YearMonth.now().toString()))
                .andExpect(jsonPath("$.receitasCentavos").value(500000))
                .andExpect(jsonPath("$.despesasCentavos").value(123456))
                .andExpect(jsonPath("$.saldoCentavos").value(376544))
                .andExpect(jsonPath("$.itens.length()").value(2));
        enviar(get("/api/lancamentos").param("mes", "2020-01"), token, null)
                .andExpect(jsonPath("$.despesasCentavos").value(9900));
        enviar(get("/api/lancamentos").param("mes", "janeiro"), token, null).andExpect(status().isBadRequest());

        enviar(get("/api/lancamentos"), outra, null).andExpect(jsonPath("$.itens.length()").value(0));
        enviar(delete("/api/lancamentos/" + mercado), outra, null).andExpect(status().isNotFound());
        enviar(delete("/api/lancamentos/" + mercado), token, null).andExpect(status().isNoContent());
    }

    private String novaCasa(String apelido) throws Exception {
        return corpo(enviar(post("/api/auth/cadastro"), null, """
                {"nome":"Teste","email":"%s@teste.com","senha":"senha-forte-1","nomeCasa":"Casa %s"}"""
                .formatted(apelido, apelido)).andExpect(status().isCreated())).get("token").asText();
    }

    private static String situacao(JsonNode agenda, String codigo) {
        for (JsonNode dose : agenda) {
            if (dose.get("codigo").asText().equals(codigo)) {
                return dose.get("situacao").asText();
            }
        }
        return null;
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
