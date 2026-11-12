package com.appfamilia.devocional.biblia;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Texto bíblico pela API.Bible (https://api.bible), que entrega traduções com a licença do detentor.
 * Configure BIBLIA_API_CHAVE (chave da sua conta) e BIBLIA_API_VERSAO (o id da tradução escolhida,
 * visto em https://api.bible/bibles). Quais traduções a sua chave pode usar depende do plano e
 * da licença de cada uma.
 */
@Component
public class FonteBiblicaApiBible implements FonteBiblica {

    private final String url;
    private final String chave;
    private final String versao;
    private final RestClient http;

    public FonteBiblicaApiBible(
            @Value("${app.biblia.url:https://rest.api.bible/v1}") String url,
            @Value("${app.biblia.chave:}") String chave,
            @Value("${app.biblia.versao:}") String versao) {
        this.url = url.trim().replaceAll("/+$", "");
        this.chave = chave == null ? "" : chave.trim();
        this.versao = versao == null ? "" : versao.trim();
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(20));
        this.http = RestClient.builder().requestFactory(fabrica).build();
    }

    @Override
    public Optional<Texto> buscar(Passagem passagem) {
        if (chave.isEmpty() || versao.isEmpty()) {
            return Optional.empty();
        }
        String livro = passagem.livro().codigo();
        String caminho;
        if (passagem.capituloInteiro()) {
            caminho = "/chapters/" + livro + "." + passagem.capitulo();
        } else {
            int fim = passagem.versiculoFim() == null ? passagem.versiculoInicio() : passagem.versiculoFim();
            caminho = "/passages/" + livro + "." + passagem.capitulo() + "." + passagem.versiculoInicio()
                    + "-" + livro + "." + passagem.capitulo() + "." + fim;
        }
        try {
            JsonNode resposta = http.get()
                    .uri(url + "/bibles/" + versao + caminho
                            + "?content-type=text&include-verse-numbers=true&include-titles=false&include-notes=false")
                    .header("api-key", chave)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode dados = resposta == null ? null : resposta.path("data");
            String conteudo = dados == null ? null : dados.path("content").asText(null);
            if (conteudo == null || conteudo.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new Texto(conteudo.trim(), dados.path("copyright").asText(null)));
        } catch (Exception falha) {
            // sem o texto, o devocional sai só com a referência
            return Optional.empty();
        }
    }
}
