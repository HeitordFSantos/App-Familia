package com.appfamilia.agenda;

import com.appfamilia.core.dependente.DependenteRepository;
import com.appfamilia.shared.erro.ErroNegocio;
import com.appfamilia.shared.seguranca.UsuarioAtual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/compromissos")
public class CompromissoController {

    public record NovoCompromisso(
            @NotBlank @Size(max = 160) String titulo,
            @Size(max = 1000) String descricao,
            @NotNull LocalDateTime inicio,
            @Min(0) Integer lembreteMinutos,
            UUID dependenteId) {
    }

    public record CompromissoResposta(UUID id, String titulo, String descricao, LocalDateTime inicio,
                                      Integer lembreteMinutos, UUID dependenteId) {
        static CompromissoResposta de(Compromisso c) {
            return new CompromissoResposta(c.getId(), c.getTitulo(), c.getDescricao(), c.getInicio(),
                    c.getLembreteMinutos(), c.getDependenteId());
        }
    }

    private final CompromissoRepository compromissos;
    private final DependenteRepository dependentes;

    public CompromissoController(CompromissoRepository compromissos, DependenteRepository dependentes) {
        this.compromissos = compromissos;
        this.dependentes = dependentes;
    }

    /** Compromissos a partir de uma data (padrão: hoje), do mais próximo ao mais distante. */
    @GetMapping
    public List<CompromissoResposta> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de) {
        LocalDateTime inicio = (de != null ? de : LocalDate.now()).atStartOfDay();
        return compromissos
                .findByCasaIdAndInicioGreaterThanEqualOrderByInicio(UsuarioAtual.de(jwt).casaId(), inicio).stream()
                .map(CompromissoResposta::de)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CompromissoResposta criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody NovoCompromisso dados) {
        UUID casaId = UsuarioAtual.de(jwt).casaId();
        if (dados.dependenteId() != null && dependentes.findByIdAndCasaId(dados.dependenteId(), casaId).isEmpty()) {
            throw new ErroNegocio(HttpStatus.BAD_REQUEST, "Dependente não encontrado nesta Casa");
        }
        String descricao = dados.descricao() == null || dados.descricao().isBlank() ? null : dados.descricao().trim();
        var salvo = compromissos.save(new Compromisso(casaId, dados.titulo().trim(), descricao,
                dados.inicio(), dados.lembreteMinutos(), dados.dependenteId()));
        return CompromissoResposta.de(salvo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void remover(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        var compromisso = compromissos.findByIdAndCasaId(id, UsuarioAtual.de(jwt).casaId())
                .orElseThrow(() -> new ErroNegocio(HttpStatus.NOT_FOUND, "Compromisso não encontrado"));
        compromissos.delete(compromisso);
    }
}
