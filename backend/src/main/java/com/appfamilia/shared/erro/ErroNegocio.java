package com.appfamilia.shared.erro;

import org.springframework.http.HttpStatus;

/** Erro previsto de regra de negócio, com o status HTTP que deve voltar para o app. */
public class ErroNegocio extends RuntimeException {

    private final HttpStatus status;

    public ErroNegocio(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
