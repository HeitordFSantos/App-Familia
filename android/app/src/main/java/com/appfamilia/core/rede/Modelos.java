package com.appfamilia.core.rede;

import java.util.List;

/** Objetos trocados com a API. Os nomes dos campos são os mesmos do JSON. */
public final class Modelos {

    private Modelos() {
    }

    public static class Cadastro {
        public String nome;
        public String email;
        public String senha;
        public String nomeCasa;
        public String conviteToken;
    }

    public static class Login {
        public String email;
        public String senha;

        public Login(String email, String senha) {
            this.email = email;
            this.senha = senha;
        }
    }

    public static class LoginGoogle {
        public String idToken;
        public String nomeCasa;
        public String conviteToken;
    }

    public static class SessaoResposta {
        public String token;
        public String membroId;
        public String casaId;
        public String nome;
        public String papel;
    }

    public static class MembroResumo {
        public String id;
        public String nome;
        public String email;
        public String papel;
    }

    public static class Casa {
        public String id;
        public String nome;
        public String meuId;
        public List<MembroResumo> membros;
    }

    public static class Dependente {
        public String id;
        public String nome;
        /** Formato AAAA-MM-DD. */
        public String dataNascimento;

        public Dependente(String nome, String dataNascimento) {
            this.nome = nome;
            this.dataNascimento = dataNascimento;
        }
    }

    public static class NovoConvite {
        public String email;

        public NovoConvite(String email) {
            this.email = email;
        }
    }

    public static class Convite {
        public String email;
        public String token;
        public String expiraEm;
    }

    public static class Erro {
        public String erro;
    }
}
