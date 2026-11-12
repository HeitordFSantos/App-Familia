package com.appfamilia.core.auth;

import com.appfamilia.core.auth.AuthDtos.Cadastro;
import com.appfamilia.core.auth.AuthDtos.Login;
import com.appfamilia.core.auth.AuthDtos.LoginGoogle;
import com.appfamilia.core.auth.AuthDtos.Sessao;
import com.appfamilia.core.auth.VerificadorGoogle.ContaGoogle;
import com.appfamilia.core.casa.Casa;
import com.appfamilia.core.casa.CasaRepository;
import com.appfamilia.core.convite.Convite;
import com.appfamilia.core.convite.ConviteRepository;
import com.appfamilia.core.membro.Membro;
import com.appfamilia.core.membro.MembroRepository;
import com.appfamilia.core.membro.Papel;
import com.appfamilia.shared.erro.ErroNegocio;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final MembroRepository membros;
    private final CasaRepository casas;
    private final ConviteRepository convites;
    private final PasswordEncoder senhas;
    private final TokenService tokens;
    private final VerificadorGoogle google;

    public AuthService(MembroRepository membros, CasaRepository casas, ConviteRepository convites,
                       PasswordEncoder senhas, TokenService tokens, VerificadorGoogle google) {
        this.membros = membros;
        this.casas = casas;
        this.convites = convites;
        this.senhas = senhas;
        this.tokens = tokens;
        this.google = google;
    }

    @Transactional
    public Sessao cadastrar(Cadastro dados) {
        String email = dados.email().trim().toLowerCase();
        if (membros.existsByEmail(email)) {
            throw new ErroNegocio(HttpStatus.CONFLICT, "Este e-mail já tem cadastro");
        }
        Membro membro = novoMembro(dados.nome().trim(), email, dados.nomeCasa(), dados.conviteToken());
        membro.setSenhaHash(senhas.encode(dados.senha()));
        return sessao(membros.save(membro));
    }

    @Transactional(readOnly = true)
    public Sessao entrar(Login dados) {
        Membro membro = membros.findByEmail(dados.email().trim().toLowerCase())
                .filter(m -> m.getSenhaHash() != null && senhas.matches(dados.senha(), m.getSenhaHash()))
                .orElseThrow(() -> new ErroNegocio(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos"));
        return sessao(membro);
    }

    /**
     * Login com Google. Três casos: conta Google já vinculada; e-mail já cadastrado
     * com senha (vincula o Google a ele); ou primeiro acesso (cria o membro).
     */
    @Transactional
    public Sessao entrarComGoogle(LoginGoogle dados) {
        ContaGoogle conta = google.verificar(dados.idToken());

        Membro membro = membros.findByGoogleSub(conta.sub()).orElse(null);
        if (membro == null) {
            membro = membros.findByEmail(conta.email()).orElse(null);
            if (membro == null) {
                membro = novoMembro(conta.nome(), conta.email(), dados.nomeCasa(), dados.conviteToken());
            }
            membro.setGoogleSub(conta.sub());
            membro = membros.save(membro);
        }
        return sessao(membro);
    }

    /** Cria o membro em uma Casa nova (vira ADMIN) ou na Casa do convite (vira MEMBRO). */
    private Membro novoMembro(String nome, String email, String nomeCasa, String conviteToken) {
        if (conviteToken != null && !conviteToken.isBlank()) {
            Convite convite = convites.findByToken(conviteToken.trim())
                    .filter(Convite::valido)
                    .filter(c -> c.getEmail().equals(email))
                    .orElseThrow(() -> new ErroNegocio(HttpStatus.BAD_REQUEST,
                            "Convite inválido, expirado ou emitido para outro e-mail"));
            convite.marcarUsado();
            return new Membro(convite.getCasa(), nome, email, Papel.MEMBRO);
        }
        if (nomeCasa == null || nomeCasa.isBlank()) {
            throw new ErroNegocio(HttpStatus.BAD_REQUEST,
                    "Informe o nome da Casa para criar uma nova, ou um convite para entrar em uma existente");
        }
        Casa casa = casas.save(new Casa(nomeCasa.trim()));
        return new Membro(casa, nome, email, Papel.ADMIN);
    }

    private Sessao sessao(Membro membro) {
        return new Sessao(tokens.gerar(membro), membro.getId(), membro.getCasa().getId(),
                membro.getNome(), membro.getPapel().name());
    }
}
