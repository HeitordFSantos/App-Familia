package com.appfamilia.core.convite;

import com.appfamilia.core.casa.CasaRepository;
import com.appfamilia.core.membro.MembroRepository;
import com.appfamilia.core.membro.Papel;
import com.appfamilia.shared.erro.ErroNegocio;
import com.appfamilia.shared.seguranca.UsuarioAtual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/convites")
public class ConviteController {

    public record NovoConvite(@NotBlank @Email String email) {
    }

    /** Por enquanto o token é devolvido na resposta; o envio por e-mail entra depois. */
    public record ConviteResposta(String email, String token, Instant expiraEm) {
    }

    private final ConviteRepository convites;
    private final CasaRepository casas;
    private final MembroRepository membros;
    private final Duration validade;

    public ConviteController(ConviteRepository convites, CasaRepository casas, MembroRepository membros,
                             @Value("${app.convite.validade-dias}") long validadeDias) {
        this.convites = convites;
        this.casas = casas;
        this.membros = membros;
        this.validade = Duration.ofDays(validadeDias);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ConviteResposta convidar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody NovoConvite dados) {
        UsuarioAtual usuario = UsuarioAtual.de(jwt);
        if (usuario.papel() != Papel.ADMIN) {
            throw new ErroNegocio(HttpStatus.FORBIDDEN, "Só o administrador da Casa pode convidar");
        }
        String email = dados.email().trim().toLowerCase();
        if (membros.existsByEmail(email)) {
            throw new ErroNegocio(HttpStatus.CONFLICT, "Este e-mail já tem cadastro");
        }
        var convite = convites.save(new Convite(
                casas.getReferenceById(usuario.casaId()),
                email,
                UUID.randomUUID().toString().replace("-", ""),
                Instant.now().plus(validade)));
        return new ConviteResposta(convite.getEmail(), convite.getToken(), convite.getExpiraEm());
    }
}
