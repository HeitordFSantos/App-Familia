package com.appfamilia;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.appfamilia.compras.perfil.Lugar;
import com.appfamilia.compras.perfil.NomeProduto;
import com.appfamilia.compras.perfil.ProdutoComumRepository;
import com.appfamilia.shared.ia.ClienteLlm;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Catálogo base, perfil da Casa e sugestões por foto que se ajustam ao uso. */
@SpringBootTest
@AutoConfigureMockMvc
class ComprasInteligentesTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    ProdutoComumRepository catalogo;

    @MockitoBean
    ClienteLlm llm;

    @Test
    void chavesDoCatalogoSeguemAMesmaRegra() {
        Assertions.assertTrue(catalogo.count() > 80);
        catalogo.findAll().forEach(p ->
                Assertions.assertEquals(NomeProduto.chave(p.getNome()), p.getChave(), p.getNome()));
        Assertions.assertEquals("feijao", NomeProduto.chave("  FEIJÕES "));
        Assertions.assertEquals("pao de forma", NomeProduto.chave("Pães de forma"));
        Assertions.assertEquals(Lugar.ARMARIO, Lugar.deTexto("Despensa"));
        Assertions.assertEquals(Lugar.FREEZER, Lugar.deTexto("freezer da geladeira"));
        Assertions.assertEquals(Lugar.OUTRO, Lugar.deTexto("mesa"));
    }

    @Test
    void sugestoesSeAjustamAoUso() throws Exception {
        String token = novaCasa("perfil");

        // 1ª foto: casa nova, sem histórico. Leite acabando, queijo e kombucha à vista.
        foto("""
                {"lugar":"Geladeira","itens":[
                  {"nome":"Leite","nivel":"acabando"},
                  {"nome":"Queijo muçarela","nivel":"ok"},
                  {"nome":"Kombucha","nivel":"ok"}]}""");
        JsonNode primeira = analisar(token);
        Assertions.assertEquals("GELADEIRA", primeira.get("lugar").asText());
        List<String> nomes = nomes(primeira);
        Assertions.assertEquals("Leite", nomes.get(0));
        Assertions.assertEquals("acabando", primeira.get("sugestoes").get(0).get("motivo").asText());
        Assertions.assertTrue(nomes.contains("Ovos"), "básico da geladeira que não apareceu");
        Assertions.assertFalse(nomes.contains("Queijo muçarela"), "apareceu na foto");
        Assertions.assertFalse(nomes.contains("Arroz"), "é de armário, não de geladeira");
        Assertions.assertFalse(nomes.contains("Kombucha"), "sem histórico suficiente ainda");

        // Usuário aceita o leite e recusa os ovos
        enviar(post("/api/compras/foto/confirmar"), token, """
                {"lugar":"GELADEIRA","aceitos":["Leite"],"recusados":["Ovos"]}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // 2ª foto igual: ovos recusados saem, leite já está na lista
        JsonNode segunda = analisar(token);
        Assertions.assertFalse(nomes(segunda).contains("Ovos"));
        Assertions.assertFalse(nomes(segunda).contains("Leite"));

        // 3ª foto sem a kombucha: vista duas vezes, agora é habitual
        foto("""
                {"lugar":"geladeira","itens":[{"nome":"Queijo muçarela","nivel":"ok"}]}""");
        JsonNode terceira = analisar(token);
        int posicao = nomes(terceira).indexOf("Kombucha");
        Assertions.assertTrue(posicao >= 0, "produto que a família costuma ter");
        Assertions.assertEquals("vocês costumam ter",
                terceira.get("sugestoes").get(posicao).get("motivo").asText());

        // O usuário desliga a kombucha na tela de habituais
        enviar(put("/api/compras/habituais/kombucha/ignorado"), token, """
                {"ignorado":true}""").andExpect(jsonPath("$.ignorado").value(true));
        Assertions.assertFalse(nomes(analisar(token)).contains("Kombucha"));
        enviar(put("/api/compras/habituais/inexistente/ignorado"), token, """
                {"ignorado":true}""").andExpect(status().isNotFound());

        // Comprar alimenta o perfil
        String arroz = corpo(enviar(post("/api/compras"), token, """
                {"nome":"arroz"}""")).get("id").asText();
        enviar(put("/api/compras/" + arroz + "/comprado"), token, """
                {"comprado":true}""").andExpect(status().isOk());
        JsonNode habituais = corpo(enviar(get("/api/compras/habituais"), token, null));
        Assertions.assertEquals("Arroz", habituais.get(0).get("nome").asText(), "comprado e adicionado lidera");
        Assertions.assertEquals(6, habituais.get(0).get("pontos").asInt(), "básico 1 + adicionado 2 + comprado 3");
    }

    @Test
    void perfilDeUmaCasaNaoAfetaOutra() throws Exception {
        String uma = novaCasa("perfil-uma");
        String outra = novaCasa("perfil-outra");
        enviar(post("/api/compras/foto/confirmar"), uma, """
                {"lugar":"GELADEIRA","aceitos":[],"recusados":["Ovos"]}""").andExpect(status().isOk());
        foto("""
                {"lugar":"geladeira","itens":[]}""");
        Assertions.assertFalse(nomes(analisar(uma)).contains("Ovos"));
        Assertions.assertTrue(nomes(analisar(outra)).contains("Ovos"));
    }

    @Test
    void fotoSemResultadoLegivel() throws Exception {
        String token = novaCasa("perfil-ilegivel");
        when(llm.lerImagem(anyString(), anyString(), any())).thenReturn("não sei");
        enviar(post("/api/compras/foto"), token, """
                {"imagemBase64":"QUJD"}""").andExpect(status().isBadGateway());
        enviar(post("/api/compras/foto"), token, """
                {"imagemBase64":""}""").andExpect(status().isBadRequest());

        // Lugar desconhecido: só vale o que aparece acabando
        foto("""
                {"lugar":"mesa","itens":[{"nome":"Pão","nivel":"acabando"}]}""");
        JsonNode analise = analisar(token);
        Assertions.assertEquals("OUTRO", analise.get("lugar").asText());
        Assertions.assertEquals(List.of("Pão"), nomes(analise));
    }

    private void foto(String respostaDoModelo) {
        when(llm.lerImagem(anyString(), anyString(), any())).thenReturn(respostaDoModelo);
    }

    private JsonNode analisar(String token) throws Exception {
        return corpo(enviar(post("/api/compras/foto"), token, """
                {"imagemBase64":"QUJD"}""").andExpect(status().isOk()));
    }

    private static List<String> nomes(JsonNode analise) {
        List<String> nomes = new ArrayList<>();
        analise.get("sugestoes").forEach(s -> nomes.add(s.get("nome").asText()));
        return nomes;
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
