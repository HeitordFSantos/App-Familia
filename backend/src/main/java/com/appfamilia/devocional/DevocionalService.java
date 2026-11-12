package com.appfamilia.devocional;

import com.appfamilia.devocional.PlanoDevocional.Tipo;
import com.appfamilia.devocional.biblia.FonteBiblica;
import com.appfamilia.devocional.biblia.FonteBiblica.Texto;
import com.appfamilia.devocional.biblia.Livros;
import com.appfamilia.devocional.biblia.Livros.Livro;
import com.appfamilia.devocional.biblia.Passagem;
import com.appfamilia.shared.erro.ErroNegocio;
import com.appfamilia.shared.ia.ClienteLlm;
import com.appfamilia.shared.ia.JsonDaLlm;
import com.appfamilia.shared.ia.Pesquisa;
import com.appfamilia.shared.ia.Pesquisa.Resultado;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Monta o devocional do dia de uma Casa, uma única vez por dia:
 * 1. escolhe a passagem pelo plano da Casa (livro em sequência ou tema);
 * 2. busca o texto exato na fonte bíblica licenciada;
 * 3. pesquisa referências na internet;
 * 4. pede ao modelo de linguagem a reflexão, com base no texto e nas referências.
 */
@Service
public class DevocionalService {

    private static final String PAPEL =
            "Você escreve devocionais cristãos curtos para famílias brasileiras, em português simples e acolhedor, "
                    + "sem defender a doutrina particular de nenhuma denominação. ";

    private final PlanoDevocionalRepository planos;
    private final DevocionalDiarioRepository diarios;
    private final ClienteLlm llm;
    private final FonteBiblica biblia;
    private final Pesquisa pesquisa;
    private final ObjectMapper json;

    public DevocionalService(PlanoDevocionalRepository planos, DevocionalDiarioRepository diarios, ClienteLlm llm,
                             FonteBiblica biblia, Pesquisa pesquisa, ObjectMapper json) {
        this.planos = planos;
        this.diarios = diarios;
        this.llm = llm;
        this.biblia = biblia;
        this.pesquisa = pesquisa;
        this.json = json;
    }

    /** Plano da Casa; se ela ainda não escolheu, começa pelos Salmos. */
    public PlanoDevocional plano(UUID casaId) {
        return planos.findByCasaId(casaId)
                .orElseGet(() -> planos.save(new PlanoDevocional(casaId, Tipo.LIVRO, "PSA")));
    }

    public PlanoDevocional trocarPlano(UUID casaId, Tipo tipo, String valor) {
        String limpo = valor == null ? "" : valor.trim();
        if (tipo == Tipo.LIVRO) {
            limpo = Livros.porCodigo(limpo)
                    .orElseThrow(() -> new ErroNegocio(HttpStatus.BAD_REQUEST, "Livro desconhecido"))
                    .codigo();
        } else if (limpo.isEmpty() || limpo.length() > 120) {
            throw new ErroNegocio(HttpStatus.BAD_REQUEST, "Informe o tema, com até 120 caracteres");
        }
        PlanoDevocional plano = plano(casaId);
        plano.trocar(tipo, limpo);
        return planos.save(plano);
    }

    public DevocionalDiario deHoje(UUID casaId) {
        LocalDate hoje = LocalDate.now();
        Optional<DevocionalDiario> pronto = diarios.findByCasaIdAndData(casaId, hoje);
        if (pronto.isPresent()) {
            return pronto.get();
        }
        if (!llm.configurado()) {
            throw new ErroNegocio(HttpStatus.SERVICE_UNAVAILABLE,
                    "O devocional precisa do modelo de linguagem, que ainda não foi configurado no servidor");
        }

        PlanoDevocional plano = plano(casaId);
        Passagem passagem = escolherPassagem(plano, casaId, hoje);
        Optional<Texto> texto = biblia.buscar(passagem);
        List<Resultado> achados = pesquisa.buscar(passagem.referencia() + " estudo bíblico reflexão", 3);
        JsonNode escrito = escrever(passagem, texto, achados, plano);

        DevocionalDiario novo = new DevocionalDiario(
                casaId, hoje, passagem.referencia(),
                texto.map(Texto::conteudo).map(t -> cortar(t, 40000)).orElse(null),
                texto.map(Texto::direitos).map(d -> cortar(d, 1000)).orElse(null),
                cortar(campo(escrito, "titulo", passagem.referencia()), 200),
                cortar(campo(escrito, "reflexao", ""), 8000),
                cortar(campo(escrito, "aplicacao", null), 2000),
                cortar(campo(escrito, "oracao", null), 2000),
                fontesJson(achados));
        try {
            novo = diarios.save(novo);
        } catch (DataIntegrityViolationException outroPedidoGravouAntes) {
            // dois membros abriram ao mesmo tempo: vale o que foi gravado primeiro
            return diarios.findByCasaIdAndData(casaId, hoje).orElseThrow();
        }
        if (plano.getTipo() == Tipo.LIVRO) {
            plano.avancar(passagem.livro().capitulos());
            planos.save(plano);
        }
        return novo;
    }

    private Passagem escolherPassagem(PlanoDevocional plano, UUID casaId, LocalDate hoje) {
        if (plano.getTipo() == Tipo.LIVRO) {
            Livro livro = Livros.porCodigo(plano.getValor()).orElse(Livros.porCodigo("PSA").orElseThrow());
            int capitulo = Math.min(Math.max(plano.getProximoCapitulo(), 1), livro.capitulos());
            return Passagem.capitulo(livro, capitulo);
        }
        String recentes = diarios.findTop15ByCasaIdOrderByDataDesc(casaId).stream()
                .map(DevocionalDiario::getReferencia)
                .collect(Collectors.joining("; "));
        String codigos = Livros.TODOS.stream().map(l -> l.codigo() + "=" + l.nome()).collect(Collectors.joining(", "));
        String resposta = llm.conversar(
                PAPEL + "Responda somente com um objeto JSON, sem nenhum texto antes ou depois.",
                "Escolha uma passagem bíblica de 3 a 15 versículos, dentro de um único capítulo, para o tema \""
                        + plano.getValor() + "\". Não repita estas passagens já usadas: "
                        + (recentes.isEmpty() ? "nenhuma" : recentes) + ".\n"
                        + "Formato: {\"livro\":\"PSA\",\"capitulo\":23,\"versiculoInicio\":1,\"versiculoFim\":6}\n"
                        + "Códigos dos livros: " + codigos);
        Passagem escolhida = lerPassagem(JsonDaLlm.extrair(json, resposta));
        if (escolhida != null) {
            return escolhida;
        }
        // O modelo não devolveu uma referência válida: usa um salmo pelo dia do ano
        Livro salmos = Livros.porCodigo("PSA").orElseThrow();
        return Passagem.capitulo(salmos, hoje.getDayOfYear() % salmos.capitulos() + 1);
    }

    /** Confere a referência que o modelo devolveu; null se ela não existir. */
    private static Passagem lerPassagem(JsonNode no) {
        if (no == null) {
            return null;
        }
        Livro livro = Livros.porCodigo(no.path("livro").asText(null)).orElse(null);
        int capitulo = no.path("capitulo").asInt(0);
        int inicio = no.path("versiculoInicio").asInt(0);
        int fim = no.path("versiculoFim").asInt(inicio);
        if (livro == null || capitulo < 1 || capitulo > livro.capitulos()
                || inicio < 1 || fim < inicio || fim > 176 || fim - inicio > 30) {
            return null;
        }
        return new Passagem(livro, capitulo, inicio, fim);
    }

    private JsonNode escrever(Passagem passagem, Optional<Texto> texto, List<Resultado> achados, PlanoDevocional plano) {
        StringBuilder pedido = new StringBuilder("Passagem de hoje: ").append(passagem.referencia()).append(".\n");
        if (plano.getTipo() == Tipo.TEMA) {
            pedido.append("Tema escolhido pela família: ").append(plano.getValor()).append(".\n");
        }
        texto.ifPresent(t -> pedido.append("Texto da passagem:\n").append(cortar(t.conteudo(), 6000)).append("\n"));
        if (!achados.isEmpty()) {
            pedido.append("Trechos de páginas sobre a passagem, só como apoio (não copie frases delas):\n");
            for (Resultado achado : achados) {
                pedido.append("- ").append(achado.titulo()).append(": ").append(cortar(achado.trecho(), 400)).append("\n");
            }
        }
        pedido.append("Escreva o devocional com suas próprias palavras. Não transcreva o texto bíblico: ele é "
                + "mostrado à parte. Formato: {\"titulo\":\"até 8 palavras\",\"reflexao\":\"3 a 4 parágrafos curtos\","
                + "\"aplicacao\":\"uma atitude prática para a família hoje\",\"oracao\":\"uma oração de 2 a 3 frases\"}");

        String resposta = llm.conversar(
                PAPEL + "Responda somente com um objeto JSON, sem nenhum texto antes ou depois.", pedido.toString());
        JsonNode lido = JsonDaLlm.extrair(json, resposta);
        if (lido != null && !lido.path("reflexao").asText("").isBlank()) {
            return lido;
        }
        // O modelo não seguiu o formato: aproveita o texto inteiro como reflexão
        return json.valueToTree(Map.of("titulo", passagem.referencia(), "reflexao", resposta));
    }

    private String fontesJson(List<Resultado> achados) {
        List<Map<String, String>> fontes = new ArrayList<>();
        for (Resultado achado : achados) {
            fontes.add(Map.of("titulo", cortar(achado.titulo(), 150), "url", achado.url()));
        }
        try {
            String texto = json.writeValueAsString(fontes);
            return texto.length() > 4000 ? "[]" : texto;
        } catch (Exception e) {
            return "[]";
        }
    }

    private static String campo(JsonNode no, String nome, String padrao) {
        String valor = no.path(nome).asText("");
        return valor.isBlank() ? padrao : valor.trim();
    }

    private static String cortar(String texto, int max) {
        return texto == null || texto.length() <= max ? texto : texto.substring(0, max);
    }
}
