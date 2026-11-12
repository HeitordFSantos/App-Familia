package com.appfamilia.core.auth;

import com.appfamilia.core.auth.AuthDtos.Cadastro;
import com.appfamilia.core.auth.AuthDtos.Login;
import com.appfamilia.core.auth.AuthDtos.LoginGoogle;
import com.appfamilia.core.auth.AuthDtos.Sessao;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints públicos: cadastro, login com senha e login com Google. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public Sessao cadastrar(@Valid @RequestBody Cadastro dados) {
        return auth.cadastrar(dados);
    }

    @PostMapping("/login")
    public Sessao entrar(@Valid @RequestBody Login dados) {
        return auth.entrar(dados);
    }

    @PostMapping("/google")
    public Sessao entrarComGoogle(@Valid @RequestBody LoginGoogle dados) {
        return auth.entrarComGoogle(dados);
    }
}
