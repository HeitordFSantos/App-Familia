package com.appfamilia.core.auth;

import com.appfamilia.shared.erro.ErroNegocio;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Valida o ID token com as chaves públicas do Google: assinatura, validade,
 * emissor e se o token foi emitido para o nosso client-id.
 */
@Component
public class VerificadorGoogleJwt implements VerificadorGoogle {

    private static final String CHAVES_GOOGLE = "https://www.googleapis.com/oauth2/v3/certs";
    private static final List<String> EMISSORES = List.of("accounts.google.com", "https://accounts.google.com");

    private final String clientId;
    private volatile NimbusJwtDecoder decoder;

    public VerificadorGoogleJwt(@Value("${app.google.client-id:}") String clientId) {
        this.clientId = clientId;
    }

    @Override
    public ContaGoogle verificar(String idToken) {
        if (clientId == null || clientId.isBlank()) {
            throw new ErroNegocio(HttpStatus.SERVICE_UNAVAILABLE, "Login com Google não está configurado no servidor");
        }
        Jwt jwt;
        try {
            jwt = decoder().decode(idToken);
        } catch (JwtException e) {
            throw new ErroNegocio(HttpStatus.UNAUTHORIZED, "Token do Google inválido");
        }
        if (!Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
            throw new ErroNegocio(HttpStatus.UNAUTHORIZED, "E-mail da conta Google não verificado");
        }
        String email = jwt.getClaimAsString("email");
        String nome = jwt.getClaimAsString("name");
        return new ContaGoogle(jwt.getSubject(), email.toLowerCase(), nome != null ? nome : email);
    }

    private NimbusJwtDecoder decoder() {
        if (decoder == null) {
            NimbusJwtDecoder novo = NimbusJwtDecoder.withJwkSetUri(CHAVES_GOOGLE).build();
            OAuth2TokenValidator<Jwt> emissorEPublico = jwt -> {
                boolean emissorOk = jwt.getClaimAsString("iss") != null && EMISSORES.contains(jwt.getClaimAsString("iss"));
                boolean publicoOk = jwt.getAudience() != null && jwt.getAudience().contains(clientId);
                return emissorOk && publicoOk
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
            };
            novo.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), emissorEPublico));
            decoder = novo;
        }
        return decoder;
    }
}
