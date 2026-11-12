package com.appfamilia.core.dependente;

import com.appfamilia.core.casa.CasaRepository;
import com.appfamilia.shared.erro.ErroNegocio;
import com.appfamilia.shared.seguranca.UsuarioAtual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dependentes")
public class DependenteController {

    public record NovoDependente(
            @NotBlank @Size(max = 120) String nome,
            @NotNull @PastOrPresent LocalDate dataNascimento) {
    }

    public record DependenteResposta(UUID id, String nome, LocalDate dataNascimento) {
        static DependenteResposta de(Dependente d) {
            return new DependenteResposta(d.getId(), d.getNome(), d.getDataNascimento());
        }
    }

    private final DependenteRepository dependentes;
    private final CasaRepository casas;

    public DependenteController(DependenteRepository dependentes, CasaRepository casas) {
        this.dependentes = dependentes;
        this.casas = casas;
    }

    @GetMapping
    public List<DependenteResposta> listar(@AuthenticationPrincipal Jwt jwt) {
        return dependentes.findByCasaIdOrderByDataNascimento(UsuarioAtual.de(jwt).casaId()).stream()
                .map(DependenteResposta::de)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public DependenteResposta criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody NovoDependente dados) {
        var casa = casas.getReferenceById(UsuarioAtual.de(jwt).casaId());
        var salvo = dependentes.save(new Dependente(casa, dados.nome().trim(), dados.dataNascimento()));
        return DependenteResposta.de(salvo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void remover(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        var dependente = dependentes.findByIdAndCasaId(id, UsuarioAtual.de(jwt).casaId())
                .orElseThrow(() -> new ErroNegocio(HttpStatus.NOT_FOUND, "Dependente não encontrado"));
        dependentes.delete(dependente);
    }
}
