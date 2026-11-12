package com.appfamilia.vacinas;

import com.appfamilia.core.dependente.Dependente;
import com.appfamilia.core.dependente.DependenteRepository;
import com.appfamilia.shared.erro.ErroNegocio;
import com.appfamilia.shared.ia.ClienteLlm;
import com.appfamilia.shared.ia.Foto;
import com.appfamilia.shared.ia.JsonDaLlm;
import com.appfamilia.shared.seguranca.UsuarioAtual;
import com.appfamilia.vacinas.CalendarioVacinal.Dose;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Leitura da caderneta de vacinação por foto. O modelo com visão aponta quais doses do
 * calendário aparecem registradas na página e em que data. Nada é gravado aqui: o app
 * mostra as sugestões e o usuário confirma cada uma, porque carimbo e letra à mão geram erro.
 */
@RestController
@RequestMapping("/api/dependentes/{dependenteId}/vacinas/foto")
public class CadernetaController {

    public record Sugestao(String codigo, String vacina, String dose, LocalDate aplicadaEm, boolean jaMarcada) {
    }

    private final DependenteRepository dependentes;
    private final RegistroVacinaRepository registros;
    private final ClienteLlm llm;
    private final ObjectMapper json;

    public CadernetaController(DependenteRepository dependentes, RegistroVacinaRepository registros,
                               ClienteLlm llm, ObjectMapper json) {
        this.dependentes = dependentes;
        this.registros = registros;
        this.llm = llm;
        this.json = json;
    }

    @PostMapping
    public List<Sugestao> ler(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID dependenteId,
                              @Valid @RequestBody Foto foto) {
        Dependente dependente = dependentes.findByIdAndCasaId(dependenteId, UsuarioAtual.de(jwt).casaId())
                .orElseThrow(() -> new ErroNegocio(HttpStatus.NOT_FOUND, "Dependente não encontrado"));

        String doses = CalendarioVacinal.DOSES.stream()
                .map(d -> d.codigo() + " = " + d.vacina() + ", " + d.dose())
                .collect(Collectors.joining("\n"));
        String resposta = llm.lerImagem(
                "Esta é a foto de uma página de caderneta de vacinação infantil brasileira. Identifique as doses "
                        + "que têm registro de aplicação (data escrita ou carimbo). Use somente os códigos da lista "
                        + "abaixo. Inclua uma dose só se a data estiver legível; na dúvida, deixe de fora.\n"
                        + "Responda somente com JSON, sem texto antes ou depois, no formato "
                        + "{\"doses\":[{\"codigo\":\"BCG_UNICA\",\"data\":\"AAAA-MM-DD\"}]}.\n"
                        + "Códigos:\n" + doses,
                foto.imagemBase64(), foto.tipo());

        JsonNode lido = JsonDaLlm.extrair(json, resposta);
        if (lido == null) {
            throw new ErroNegocio(HttpStatus.BAD_GATEWAY,
                    "Não foi possível ler a caderneta nesta foto. Tente uma foto mais nítida, de uma página por vez.");
        }
        Set<String> jaMarcadas = registros.findByDependenteId(dependenteId).stream()
                .map(RegistroVacina::getCodigoDose)
                .collect(Collectors.toSet());
        LocalDate hoje = LocalDate.now();
        Set<String> vistos = new HashSet<>();
        List<Sugestao> sugestoes = new ArrayList<>();
        for (JsonNode item : lido.path("doses")) {
            Dose dose = dose(item.path("codigo").asText(""));
            LocalDate data = data(item.path("data").asText(""));
            // Descarta o que o modelo inventou: código fora do calendário ou data impossível
            if (dose == null || data == null || data.isAfter(hoje)
                    || data.isBefore(dependente.getDataNascimento()) || !vistos.add(dose.codigo())) {
                continue;
            }
            sugestoes.add(new Sugestao(dose.codigo(), dose.vacina(), dose.dose(), data,
                    jaMarcadas.contains(dose.codigo())));
        }
        return sugestoes;
    }

    private static Dose dose(String codigo) {
        return CalendarioVacinal.DOSES.stream().filter(d -> d.codigo().equals(codigo.trim())).findFirst().orElse(null);
    }

    private static LocalDate data(String texto) {
        try {
            return LocalDate.parse(texto.trim());
        } catch (Exception invalida) {
            return null;
        }
    }
}
