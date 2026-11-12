package com.appfamilia.core.auth;

/** Confere um ID token do Google e devolve os dados da conta. */
public interface VerificadorGoogle {

    record ContaGoogle(String sub, String email, String nome) {
    }

    /** Lança ErroNegocio (401) se o token for inválido. */
    ContaGoogle verificar(String idToken);
}
