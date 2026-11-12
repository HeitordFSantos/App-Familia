package com.appfamilia;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.appfamilia.core.auth.VerificadorGoogle;
import com.appfamilia.core.auth.VerificadorGoogle.ContaGoogle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Percorre o fluxo que valida o núcleo: cadastro, login, dependente, convite e isolamento entre Casas. */
@SpringBootTest
@AutoConfigureMockMvc
class NucleoIntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @MockitoBean
    VerificadorGoogle google;

    @Test
    void fluxoCompletoDoNucleo() throws Exception {
        // 1. Cadastro cria o membro e a Casa
        JsonNode admin = corpo(enviar("/api/auth/cadastro", null, """
                {"nome":"Ana","email":"ana@teste.com","senha":"senha-forte-1","nomeCasa":"Casa da Ana"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.papel").value("ADMIN")));
        String tokenAdmin = admin.get("token").asText();

        // 2. Login devolve token; senha errada é recusada
        enviar("/api/auth/login", null, """
                {"email":"ana@teste.com","senha":"senha-forte-1"}""").andExpect(status().isOk());
        enviar("/api/auth/login", null, """
                {"email":"ana@teste.com","senha":"errada"}""").andExpect(status().isUnauthorized());

        // 3. Sem token não entra; com token cadastra dependente
        mvc.perform(get("/api/dependentes")).andExpect(status().isUnauthorized());
        enviar("/api/dependentes", tokenAdmin, """
                {"nome":"Bia","dataNascimento":"2024-03-10"}""").andExpect(status().isCreated());

        // 4. Convite e entrada do segundo adulto na mesma Casa
        String convite = corpo(enviar("/api/convites", tokenAdmin, """
                {"email":"rui@teste.com"}""").andExpect(status().isCreated())).get("token").asText();
        JsonNode segundo = corpo(enviar("/api/auth/cadastro", null, """
                {"nome":"Rui","email":"rui@teste.com","senha":"senha-forte-2","conviteToken":"%s"}""".formatted(convite))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.papel").value("MEMBRO"))
                .andExpect(jsonPath("$.casaId").value(admin.get("casaId").asText())));
        String tokenSegundo = segundo.get("token").asText();

        mvc.perform(get("/api/dependentes").header("Authorization", "Bearer " + tokenSegundo))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Bia"));
        mvc.perform(get("/api/casa").header("Authorization", "Bearer " + tokenSegundo))
                .andExpect(jsonPath("$.membros.length()").value(2));

        // Convite não pode ser reutilizado; membro comum não convida
        enviar("/api/auth/cadastro", null, """
                {"nome":"X","email":"rui@teste.com","senha":"senha-forte-3","conviteToken":"%s"}""".formatted(convite))
                .andExpect(status().isConflict());
        enviar("/api/convites", tokenSegundo, """
                {"email":"outro@teste.com"}""").andExpect(status().isForbidden());

        // 5. Outra Casa não enxerga os dados da primeira
        String tokenOutraCasa = corpo(enviar("/api/auth/cadastro", null, """
                {"nome":"Leo","email":"leo@teste.com","senha":"senha-forte-4","nomeCasa":"Casa do Leo"}"""))
                .get("token").asText();
        mvc.perform(get("/api/dependentes").header("Authorization", "Bearer " + tokenOutraCasa))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void loginComGoogle() throws Exception {
        when(google.verificar("token-novo")).thenReturn(new ContaGoogle("sub-1", "gil@teste.com", "Gil"));

        // Primeiro acesso sem dizer a Casa: recusado
        enviar("/api/auth/google", null, """
                {"idToken":"token-novo"}""").andExpect(status().isBadRequest());

        // Primeiro acesso criando a Casa, depois login direto
        String casaId = corpo(enviar("/api/auth/google", null, """
                {"idToken":"token-novo","nomeCasa":"Casa do Gil"}""").andExpect(status().isOk()))
                .get("casaId").asText();
        enviar("/api/auth/google", null, """
                {"idToken":"token-novo"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.casaId").value(casaId));

        // Conta criada com senha passa a aceitar o Google do mesmo e-mail
        String casaSenha = corpo(enviar("/api/auth/cadastro", null, """
                {"nome":"Eva","email":"eva@teste.com","senha":"senha-forte-5","nomeCasa":"Casa da Eva"}"""))
                .get("casaId").asText();
        when(google.verificar("token-eva")).thenReturn(new ContaGoogle("sub-2", "eva@teste.com", "Eva"));
        enviar("/api/auth/google", null, """
                {"idToken":"token-eva"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.casaId").value(casaSenha));
    }

    private ResultActions enviar(String caminho, String token, String corpo) throws Exception {
        var requisicao = post(caminho).contentType(MediaType.APPLICATION_JSON).content(corpo);
        if (token != null) {
            requisicao.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(requisicao);
    }

    private JsonNode corpo(ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsString());
    }
}
