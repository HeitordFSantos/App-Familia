package com.appfamilia.shared.seguranca;

import com.appfamilia.core.membro.Papel;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Quem está fazendo a requisição, lido do token. Todo módulo usa o casaId
 * daqui para filtrar dados, nunca um casaId vindo do corpo da requisição.
 */
public record UsuarioAtual(UUID membroId, UUID casaId, Papel papel) {

    public static UsuarioAtual de(Jwt jwt) {
        return new UsuarioAtual(
                UUID.fromString(jwt.getSubject()),
                UUID.fromString(jwt.getClaimAsString("casa")),
                Papel.valueOf(jwt.getClaimAsString("papel")));
    }
}
