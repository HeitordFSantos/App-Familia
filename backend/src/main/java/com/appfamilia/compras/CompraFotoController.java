package com.appfamilia.compras;

import com.appfamilia.compras.CompraController.ItemResposta;
import com.appfamilia.compras.perfil.Lugar;
import com.appfamilia.compras.perfil.NomeProduto;
import com.appfamilia.compras.perfil.PerfilComprasService;
import com.appfamilia.compras.perfil.PerfilComprasService.Candidato;
import com.appfamilia.compras.perfil.PerfilComprasService.Sugestao;
import com.appfamilia.compras.perfil.PerfilComprasService.Visto;
import com.appfamilia.compras.perfil.ProdutoCasa.Evento;
import com.appfamilia.shared.erro.ErroNegocio;
import com.appfamilia.shared.ia.ClienteLlm;
import com.appfamilia.shared.ia.Foto;
import com.appfamilia.shared.ia.JsonDaLlm;
import com.appfamilia.shared.seguranca.UsuarioAtual;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lista de compras a partir de uma foto de armário, geladeira etc.
 *
 * O modelo só faz o que enxerga: diz que lugar é e o que está visível, marcando o que está
 * acabando. Quem decide o que sugerir é o PerfilComprasService, com o histórico da Casa.
 * Depois que o usuário confere, /confirmar grava o que foi aceito e o que foi recusado, e é
 * assim que as próximas sugestões se ajustam aos hábitos da família.
 */
@RestController
@RequestMapping("/api/compras/foto")
public class CompraFotoController {

    /** Quantos nomes conhecidos vão no pedido ao modelo, para ele usar os mesmos nomes. */
    private static final int NOMES_NO_PEDIDO = 150;

    public record Analise(String lugar, List<Sugestao> sugestoes) {
    }

    public record Confirmacao(
            String lugar,
            @Size(max = 60) List<@Size(max = 160) String> aceitos,
            @Size(max = 60) List<@Size(max = 160) String> recusados) {
    }

    private final ClienteLlm llm;
    private final ObjectMapper json;
    private final PerfilComprasService perfil;
    private final ItemCompraRepository itens;

    public CompraFotoController(ClienteLlm llm, ObjectMapper json, PerfilComprasService perfil,
                                ItemCompraRepository itens) {
        this.llm = llm;
        this.json = json;
        this.perfil = perfil;
        this.itens = itens;
    }

    @PostMapping
    public Analise analisar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Foto foto) {
        UUID casaId = UsuarioAtual.de(jwt).casaId();
        List<Candidato> candidatos = perfil.candidatos(casaId);
        String conhecidos = candidatos.stream()
                .filter(c -> !c.ignorado())
                .limit(NOMES_NO_PEDIDO)
                .map(Candidato::nome)
                .collect(Collectors.joining(", "));

        String resposta = llm.lerImagem(
                "Esta é a foto de um espaço de uma casa brasileira onde se guardam produtos. Diga que lugar é e "
                        + "liste os produtos que aparecem. Para cada produto, diga se está \"acabando\" (embalagem "
                        + "quase vazia, último item) ou \"ok\". Liste só o que dá para ver; não invente.\n"
                        + "Quando um produto corresponder a um destes nomes, use exatamente o nome da lista: "
                        + conhecidos + ".\n"
                        + "Responda somente com JSON, sem texto antes ou depois, no formato "
                        + "{\"lugar\":\"geladeira\",\"itens\":[{\"nome\":\"Leite\",\"nivel\":\"acabando\"}]}. "
                        + "lugar é um de: geladeira, freezer, armario, limpeza, higiene, outro.",
                foto.imagemBase64(), foto.tipo());

        JsonNode lido = JsonDaLlm.extrair(json, resposta);
        if (lido == null) {
            throw new ErroNegocio(HttpStatus.BAD_GATEWAY, "Não foi possível analisar esta foto. Tente outra, mais nítida.");
        }
        Lugar lugar = Lugar.deTexto(lido.path("lugar").asText(""));
        List<Visto> vistos = new ArrayList<>();
        for (JsonNode item : lido.path("itens")) {
            String nome = item.path("nome").asText("").trim();
            if (!nome.isEmpty() && nome.length() <= NomeProduto.TAMANHO_MAXIMO) {
                vistos.add(new Visto(nome, "acabando".equalsIgnoreCase(item.path("nivel").asText("").trim())));
            }
        }

        // O que aparece na foto é o que a família guarda: conta para o perfil
        for (Visto visto : vistos) {
            perfil.registrar(casaId, visto.nome(), lugar, Evento.VISTO);
        }
        List<Candidato> atualizados = perfil.candidatos(casaId);
        return new Analise(lugar.name(), perfil.sugerir(atualizados, lugar, vistos, chavesNaLista(casaId)));
    }

    /** Grava a conferência do usuário: aceitos entram na lista, recusados baixam a pontuação. */
    @PostMapping("/confirmar")
    @Transactional
    public List<ItemResposta> confirmar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Confirmacao dados) {
        UUID casaId = UsuarioAtual.de(jwt).casaId();
        Lugar lugar = dados.lugar() == null ? null : Lugar.deTexto(dados.lugar());
        Set<String> naLista = chavesNaLista(casaId);
        List<ItemResposta> criados = new ArrayList<>();
        for (String nome : dados.aceitos() == null ? List.<String>of() : dados.aceitos()) {
            String chave = NomeProduto.chave(nome);
            if (chave.isEmpty() || !naLista.add(chave)) {
                continue;
            }
            criados.add(ItemResposta.de(itens.save(new ItemCompra(casaId, NomeProduto.exibicao(nome), null))));
            perfil.registrar(casaId, nome, lugar, Evento.ADICIONADO);
        }
        for (String nome : dados.recusados() == null ? List.<String>of() : dados.recusados()) {
            perfil.registrar(casaId, nome, lugar, Evento.RECUSADO);
        }
        return criados;
    }

    private Set<String> chavesNaLista(UUID casaId) {
        return itens.findByCasaIdOrderByCompradoAscCriadoEmAsc(casaId).stream()
                .filter(i -> !i.isComprado())
                .map(i -> NomeProduto.chave(i.getNome()))
                .collect(Collectors.toSet());
    }
}
