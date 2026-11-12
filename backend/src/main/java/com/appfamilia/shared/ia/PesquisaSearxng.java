package com.appfamilia.shared.ia;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Pesquisa por uma instância do SearXNG (buscador open source que você mesmo hospeda).
 * A instância precisa ter o formato JSON habilitado. Configure PESQUISA_URL, por exemplo
 * http://localhost:8888. Sem a configuração, a pesquisa simplesmente não devolve nada.
 */
@Component
public class PesquisaSearxng implements Pesquisa {

    private final String url;
    private final RestClient http;

    public PesquisaSearxng(@Value("${app.pesquisa.url:}") String url) {
        this.url = url == null ? "" : url.trim().replaceAll("/+$", "");
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(20));
        this.http = RestClient.builder().requestFactory(fabrica).build();
    }

    @Override
    public List<Resultado> buscar(String consulta, int max) {
        List<Resultado> achados = new ArrayList<>();
        if (url.isBlank()) {
            return achados;
        }
        try {
            var endereco = UriComponentsBuilder.fromUriString(url + "/search")
                    .queryParam("q", consulta)
                    .queryParam("format", "json")
                    .queryParam("language", "pt-BR")
                    .build()
                    .encode()
                    .toUri();
            JsonNode resposta = http.get().uri(endereco).retrieve().body(JsonNode.class);
            if (resposta != null) {
                for (JsonNode item : resposta.path("results")) {
                    String link = item.path("url").asText("");
                    if (link.startsWith("http") && achados.size() < max) {
                        achados.add(new Resultado(item.path("title").asText(link), link,
                                item.path("content").asText("")));
                    }
                }
            }
        } catch (Exception falha) {
            // pesquisa é um complemento: se falhar, o devocional sai sem referências externas
        }
        return achados;
    }
}
