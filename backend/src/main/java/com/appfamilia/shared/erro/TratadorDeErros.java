package com.appfamilia.shared.erro;

import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Devolve todos os erros no mesmo formato: {"erro": "mensagem"}. */
@RestControllerAdvice
public class TratadorDeErros {

    public record Erro(String erro) {
    }

    @ExceptionHandler(ErroNegocio.class)
    public ResponseEntity<Erro> negocio(ErroNegocio e) {
        return ResponseEntity.status(e.getStatus()).body(new Erro(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Erro> validacao(MethodArgumentNotValidException e) {
        String mensagem = e.getBindingResult().getFieldErrors().stream()
                .map(campo -> campo.getField() + ": " + campo.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Erro(mensagem));
    }
}
