package com.appfamilia.compras;

import com.appfamilia.shared.erro.ErroNegocio;
import com.appfamilia.shared.ia.ClienteLlm;
import com.appfamilia.shared.ia.Foto;
import com.appfamilia.shared.ia.JsonDaLlm;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sugestões de compra a partir de uma foto de armário ou geladeira. O modelo só enxerga o que
 * aparece na foto e não sabe o que a família costuma ter, então devolve sugestões: o app mostra
 * a lista e o usuário escolhe o que entra na lista de compras.
 */
@RestController
@RequestMapping("/api/compras/foto")
public class CompraFotoController {

    private static final int MAXIMO = 30;

    public record Sugestao(String nome, String motivo) {
    }

    private final ClienteLlm llm;
    private final ObjectMapper json;

    public CompraFotoController(ClienteLlm llm, ObjectMapper json) {
        this.llm = llm;
        this.json = json;
    }

    @PostMapping
    public List<Sugestao> sugerir(@Valid @RequestBody Foto foto) {
        String resposta = llm.lerImagem(
                "Esta é a foto de um armário de cozinha, despensa ou geladeira de uma casa brasileira. Liste os "
                        + "produtos que parecem estar acabando (embalagem quase vazia, último item) e os itens "
                        + "básicos desse tipo de espaço que claramente não aparecem. Não liste o que está bem "
                        + "abastecido. Use nomes comuns de produto, sem marca.\n"
                        + "Responda somente com JSON, sem texto antes ou depois, no formato "
                        + "{\"itens\":[{\"nome\":\"Leite\",\"motivo\":\"acabando\"}]}. "
                        + "O motivo é \"acabando\" ou \"não aparece\".",
                foto.imagemBase64(), foto.tipo());

        JsonNode lido = JsonDaLlm.extrair(json, resposta);
        if (lido == null) {
            throw new ErroNegocio(HttpStatus.BAD_GATEWAY, "Não foi possível analisar esta foto. Tente outra, mais nítida.");
        }
        List<Sugestao> sugestoes = new ArrayList<>();
        for (JsonNode item : lido.path("itens")) {
            String nome = item.path("nome").asText("").trim();
            if (!nome.isEmpty() && nome.length() <= 160 && sugestoes.size() < MAXIMO) {
                sugestoes.add(new Sugestao(nome, item.path("motivo").asText("")));
            }
        }
        return sugestoes;
    }
}
