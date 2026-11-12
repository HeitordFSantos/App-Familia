package com.appfamilia.core.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Corpos de requisição e resposta dos endpoints de autenticação. */
public final class AuthDtos {

    private AuthDtos() {
    }

    /**
     * Cadastro com e-mail e senha. Informe nomeCasa para criar uma Casa nova,
     * ou conviteToken para entrar em uma Casa existente.
     */
    public record Cadastro(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String senha,
            @Size(max = 120) String nomeCasa,
            String conviteToken) {
    }

    public record Login(@NotBlank @Email String email, @NotBlank String senha) {
    }

    /**
     * Login com Google. idToken é o token que o app Android recebe do Google.
     * No primeiro acesso, informe nomeCasa ou conviteToken, como no cadastro.
     */
    public record LoginGoogle(
            @NotBlank String idToken,
            @Size(max = 120) String nomeCasa,
            String conviteToken) {
    }

    public record Sessao(String token, UUID membroId, UUID casaId, String nome, String papel) {
    }
}
