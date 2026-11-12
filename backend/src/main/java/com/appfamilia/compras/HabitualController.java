package com.appfamilia.compras;

import com.appfamilia.compras.perfil.PerfilComprasService;
import com.appfamilia.compras.perfil.PerfilComprasService.Candidato;
import com.appfamilia.shared.erro.ErroNegocio;
import com.appfamilia.shared.seguranca.UsuarioAtual;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Produtos que a Casa conhece, com a pontuação, e a opção de nunca mais sugerir um deles. */
@RestController
@RequestMapping("/api/compras/habituais")
public class HabitualController {

    public record Habitual(String chave, String nome, String lugar, int pontos, boolean ignorado,
                           boolean sugerivel) {
        static Habitual de(Candidato c) {
            return new Habitual(c.chave(), c.nome(), c.lugar() == null ? null : c.lugar().name(), c.pontos(),
                    c.ignorado(), c.sugerivel());
        }
    }

    public record Ignorar(boolean ignorado) {
    }

    private final PerfilComprasService perfil;

    public HabitualController(PerfilComprasService perfil) {
        this.perfil = perfil;
    }

    @GetMapping
    public List<Habitual> listar(@AuthenticationPrincipal Jwt jwt) {
        return perfil.candidatos(UsuarioAtual.de(jwt).casaId()).stream().map(Habitual::de).toList();
    }

    @PutMapping("/{chave}/ignorado")
    public Habitual ignorar(@AuthenticationPrincipal Jwt jwt, @PathVariable String chave,
                            @RequestBody Ignorar dados) {
        UUID casaId = UsuarioAtual.de(jwt).casaId();
        if (!perfil.ignorar(casaId, chave, dados.ignorado())) {
            throw new ErroNegocio(HttpStatus.NOT_FOUND, "Produto não encontrado");
        }
        return perfil.candidatos(casaId).stream()
                .filter(c -> c.chave().equals(chave))
                .findFirst()
                .map(Habitual::de)
                .orElseThrow();
    }
}
