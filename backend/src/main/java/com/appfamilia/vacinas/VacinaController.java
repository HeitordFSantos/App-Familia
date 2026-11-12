package com.appfamilia.vacinas;

import com.appfamilia.core.dependente.Dependente;
import com.appfamilia.core.dependente.DependenteRepository;
import com.appfamilia.shared.erro.ErroNegocio;
import com.appfamilia.shared.seguranca.UsuarioAtual;
import com.appfamilia.vacinas.CalendarioVacinal.Dose;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Agenda vacinal de um filho: o calendário cruzado com a data de nascimento e as doses já marcadas. */
@RestController
@RequestMapping("/api/dependentes/{dependenteId}/vacinas")
public class VacinaController {

    /** Dias de antecedência para uma dose aparecer como PROXIMA. */
    private static final int DIAS_PROXIMA = 30;

    public record Aplicacao(@NotNull @PastOrPresent LocalDate aplicadaEm) {
    }

    /** situacao: APLICADA, PENDENTE (idade já chegou e não foi marcada), PROXIMA ou FUTURA. */
    public record DoseResposta(String codigo, String vacina, String dose, int idadeMeses, String observacao,
                               LocalDate dataPrevista, String situacao, LocalDate aplicadaEm) {
    }

    private final DependenteRepository dependentes;
    private final RegistroVacinaRepository registros;

    public VacinaController(DependenteRepository dependentes, RegistroVacinaRepository registros) {
        this.dependentes = dependentes;
        this.registros = registros;
    }

    @GetMapping
    public List<DoseResposta> agenda(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID dependenteId) {
        Dependente dependente = buscar(jwt, dependenteId);
        Map<String, LocalDate> aplicadas = registros.findByDependenteId(dependenteId).stream()
                .collect(Collectors.toMap(RegistroVacina::getCodigoDose, RegistroVacina::getAplicadaEm));
        LocalDate hoje = LocalDate.now();
        return CalendarioVacinal.DOSES.stream()
                .map(dose -> resposta(dose, dependente.getDataNascimento(), aplicadas.get(dose.codigo()), hoje))
                .toList();
    }

    @PutMapping("/{codigo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void marcarAplicada(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID dependenteId,
                               @PathVariable String codigo, @Valid @RequestBody Aplicacao dados) {
        Dependente dependente = buscar(jwt, dependenteId);
        if (!CalendarioVacinal.existe(codigo)) {
            throw new ErroNegocio(HttpStatus.NOT_FOUND, "Dose não existe no calendário");
        }
        if (dados.aplicadaEm().isBefore(dependente.getDataNascimento())) {
            throw new ErroNegocio(HttpStatus.BAD_REQUEST, "A data da aplicação é anterior ao nascimento");
        }
        registros.findByDependenteIdAndCodigoDose(dependenteId, codigo).ifPresentOrElse(
                registro -> registro.setAplicadaEm(dados.aplicadaEm()),
                () -> registros.save(new RegistroVacina(
                        UsuarioAtual.de(jwt).casaId(), dependenteId, codigo, dados.aplicadaEm())));
    }

    @DeleteMapping("/{codigo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void desmarcar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID dependenteId,
                          @PathVariable String codigo) {
        buscar(jwt, dependenteId);
        registros.findByDependenteIdAndCodigoDose(dependenteId, codigo).ifPresent(registros::delete);
    }

    private Dependente buscar(Jwt jwt, UUID dependenteId) {
        return dependentes.findByIdAndCasaId(dependenteId, UsuarioAtual.de(jwt).casaId())
                .orElseThrow(() -> new ErroNegocio(HttpStatus.NOT_FOUND, "Dependente não encontrado"));
    }

    private static DoseResposta resposta(Dose dose, LocalDate nascimento, LocalDate aplicadaEm, LocalDate hoje) {
        LocalDate prevista = nascimento.plusMonths(dose.idadeMeses());
        String situacao;
        if (aplicadaEm != null) {
            situacao = "APLICADA";
        } else if (!prevista.isAfter(hoje)) {
            situacao = "PENDENTE";
        } else if (!prevista.isAfter(hoje.plusDays(DIAS_PROXIMA))) {
            situacao = "PROXIMA";
        } else {
            situacao = "FUTURA";
        }
        return new DoseResposta(dose.codigo(), dose.vacina(), dose.dose(), dose.idadeMeses(), dose.observacao(),
                prevista, situacao, aplicadaEm);
    }
}
