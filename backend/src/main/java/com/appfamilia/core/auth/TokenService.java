package com.appfamilia.core.auth;

import com.appfamilia.core.membro.Membro;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Emite o token de sessão. O token carrega o membro, a Casa e o papel. */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final Duration validade;

    public TokenService(JwtEncoder encoder, @Value("${app.jwt.validade-horas}") long validadeHoras) {
        this.encoder = encoder;
        this.validade = Duration.ofHours(validadeHoras);
    }

    public String gerar(Membro membro) {
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(membro.getId().toString())
                .claim("casa", membro.getCasa().getId().toString())
                .claim("papel", membro.getPapel().name())
                .issuedAt(agora)
                .expiresAt(agora.plus(validade))
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
    }
}
