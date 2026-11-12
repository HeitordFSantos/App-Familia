package com.appfamilia.shared.ia;

import com.appfamilia.shared.erro.ErroNegocio;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Fala o protocolo "chat completions" no formato da OpenAI, que o Ollama, o llama.cpp e os
 * serviços que hospedam Llama aceitam. Para usar Ollama no próprio computador:
 * LLM_URL=http://localhost:11434/v1, LLM_MODELO=llama3.2, LLM_MODELO_VISAO=llama3.2-vision.
 */
@Component
public class ClienteLlmCompativelOpenAi implements ClienteLlm {

    private final String url;
    private final String chave;
    private final String modelo;
    private final String modeloVisao;
    private final RestClient http;

    public ClienteLlmCompativelOpenAi(
            @Value("${app.llm.url:}") String url,
            @Value("${app.llm.chave:}") String chave,
            @Value("${app.llm.modelo:llama3.2}") String modelo,
            @Value("${app.llm.modelo-visao:llama3.2-vision}") String modeloVisao) {
        this.url = url == null ? "" : url.trim().replaceAll("/+$", "");
        this.chave = chave;
        this.modelo = modelo;
        this.modeloVisao = modeloVisao;
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(10));
        fabrica.setReadTimeout(Duration.ofSeconds(180)); // modelo local pode demorar
        this.http = RestClient.builder().requestFactory(fabrica).build();
    }

    @Override
    public boolean configurado() {
        return !url.isBlank();
    }

    @Override
    public String conversar(String instrucoes, String mensagem) {
        return enviar(modelo, List.of(
                Map.of("role", "system", "content", instrucoes),
                Map.of("role", "user", "content", mensagem)));
    }

    @Override
    public String lerImagem(String instrucoes, String imagemBase64, String tipoMime) {
        String tipo = tipoMime == null || tipoMime.isBlank() ? "image/jpeg" : tipoMime;
        return enviar(modeloVisao, List.of(Map.of("role", "user", "content", List.of(
                Map.of("type", "text", "text", instrucoes),
                Map.of("type", "image_url", "image_url",
                        Map.of("url", "data:" + tipo + ";base64," + imagemBase64))))));
    }

    private String enviar(String qualModelo, List<?> mensagens) {
        if (!configurado()) {
            throw new ErroNegocio(HttpStatus.SERVICE_UNAVAILABLE,
                    "O modelo de linguagem não está configurado no servidor (LLM_URL)");
        }
        try {
            var requisicao = http.post()
                    .uri(url + "/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON);
            if (chave != null && !chave.isBlank()) {
                requisicao = requisicao.header("Authorization", "Bearer " + chave);
            }
            JsonNode resposta = requisicao
                    .body(Map.of("model", qualModelo, "messages", mensagens, "temperature", 0.4))
                    .retrieve()
                    .body(JsonNode.class);
            String texto = resposta == null ? null
                    : resposta.path("choices").path(0).path("message").path("content").asText(null);
            if (texto == null || texto.isBlank()) {
                throw new ErroNegocio(HttpStatus.BAD_GATEWAY, "O modelo de linguagem devolveu uma resposta vazia");
            }
            return texto;
        } catch (RestClientException e) {
            throw new ErroNegocio(HttpStatus.BAD_GATEWAY,
                    "Não foi possível falar com o modelo de linguagem. Confira se ele está no ar.");
        }
    }
}
