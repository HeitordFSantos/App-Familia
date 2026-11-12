package com.appfamilia.core.rede;

import java.util.List;

/** Objetos trocados com a API. Os nomes dos campos são os mesmos do JSON. */
public final class Modelos {

    private Modelos() {
    }

    // ----- Autenticação e núcleo -----

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

    // ----- Agenda -----

    public static class Compromisso {
        public String id;
        public String titulo;
        public String descricao;
        /** Data e hora locais, formato AAAA-MM-DDTHH:MM:SS. */
        public String inicio;
        /** Minutos antes do início; nulo = sem lembrete. */
        public Integer lembreteMinutos;
        public String dependenteId;
    }

    // ----- Vacinas -----

    public static class DoseVacina {
        public String codigo;
        public String vacina;
        public String dose;
        public int idadeMeses;
        public String observacao;
        public String dataPrevista;
        /** APLICADA, PENDENTE, PROXIMA ou FUTURA. */
        public String situacao;
        public String aplicadaEm;
    }

    public static class Aplicacao {
        public String aplicadaEm;

        public Aplicacao(String aplicadaEm) {
            this.aplicadaEm = aplicadaEm;
        }
    }

    // ----- Compras -----

    public static class ItemCompra {
        public String id;
        public String nome;
        public String quantidade;
        public boolean comprado;

        public ItemCompra(String nome, String quantidade) {
            this.nome = nome;
            this.quantidade = quantidade;
        }
    }

    public static class Comprado {
        public boolean comprado;

        public Comprado(boolean comprado) {
            this.comprado = comprado;
        }
    }

    // ----- Finanças -----

    public static class Lancamento {
        public String id;
        /** RECEITA ou DESPESA. */
        public String tipo;
        public long valorCentavos;
        public String categoria;
        public String descricao;
        /** Formato AAAA-MM-DD. */
        public String data;
    }

    public static class ResumoMes {
        public String mes;
        public long receitasCentavos;
        public long despesasCentavos;
        public long saldoCentavos;
        public List<Lancamento> itens;
    }

    // ----- Fotos lidas pelo modelo de linguagem -----

    public static class Foto {
        public String imagemBase64;
        public String tipo = "image/jpeg";

        public Foto(String imagemBase64) {
            this.imagemBase64 = imagemBase64;
        }
    }

    public static class SugestaoVacina {
        public String codigo;
        public String vacina;
        public String dose;
        public String aplicadaEm;
        public boolean jaMarcada;
    }

    public static class SugestaoCompra {
        public String nome;
        /** "acabando", "vocês costumam ter" ou "item básico". */
        public String motivo;
    }

    /** Resultado da foto: o lugar reconhecido e as sugestões montadas com o histórico da Casa. */
    public static class AnaliseFoto {
        public String lugar;
        public List<SugestaoCompra> sugestoes;
    }

    /** O que o usuário fez com as sugestões. Os recusados deixam de ser sugeridos com o tempo. */
    public static class Confirmacao {
        public String lugar;
        public List<String> aceitos;
        public List<String> recusados;

        public Confirmacao(String lugar, List<String> aceitos, List<String> recusados) {
            this.lugar = lugar;
            this.aceitos = aceitos;
            this.recusados = recusados;
        }
    }

    public static class ItemHabitual {
        public String chave;
        public String nome;
        public String lugar;
        public int pontos;
        public boolean ignorado;
        public boolean sugerivel;
    }

    public static class Ignorado {
        public boolean ignorado;

        public Ignorado(boolean ignorado) {
            this.ignorado = ignorado;
        }
    }

    // ----- Devocional -----

    public static class Fonte {
        public String titulo;
        public String url;
    }

    public static class DevocionalDia {
        public String data;
        public String referencia;
        /** Nulo quando o servidor não tem uma tradução licenciada configurada. */
        public String textoBiblico;
        public String direitos;
        public String titulo;
        public String reflexao;
        public String aplicacao;
        public String oracao;
        public List<Fonte> fontes;
    }

    public static class Plano {
        /** LIVRO ou TEMA. */
        public String tipo;
        public String valor;
        public String descricao;
        public int proximoCapitulo;

        public Plano(String tipo, String valor) {
            this.tipo = tipo;
            this.valor = valor;
        }
    }

    public static class LivroBiblia {
        public String codigo;
        public String nome;
        public int capitulos;
    }
}
