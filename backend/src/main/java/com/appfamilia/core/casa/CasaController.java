package com.appfamilia.core.casa;

import com.appfamilia.core.membro.MembroRepository;
import com.appfamilia.shared.erro.ErroNegocio;
import com.appfamilia.shared.seguranca.UsuarioAtual;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Dados da Casa do usuário logado e de quem faz parte dela. */
@RestController
@RequestMapping("/api/casa")
public class CasaController {

    public record MembroResumo(UUID id, String nome, String email, String papel) {
    }

    public record CasaResposta(UUID id, String nome, UUID meuId, List<MembroResumo> membros) {
    }

    private final CasaRepository casas;
    private final MembroRepository membros;

    public CasaController(CasaRepository casas, MembroRepository membros) {
        this.casas = casas;
        this.membros = membros;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public CasaResposta minhaCasa(@AuthenticationPrincipal Jwt jwt) {
        UsuarioAtual usuario = UsuarioAtual.de(jwt);
        Casa casa = casas.findById(usuario.casaId())
                .orElseThrow(() -> new ErroNegocio(HttpStatus.NOT_FOUND, "Casa não encontrada"));
        List<MembroResumo> lista = membros.findByCasaIdOrderByNome(casa.getId()).stream()
                .map(m -> new MembroResumo(m.getId(), m.getNome(), m.getEmail(), m.getPapel().name()))
                .toList();
        return new CasaResposta(casa.getId(), casa.getNome(), usuario.membroId(), lista);
    }
}
