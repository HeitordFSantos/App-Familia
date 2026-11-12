package com.appfamilia.devocional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** O que a Casa escolheu ler: um livro em sequência (um capítulo por dia) ou um tema. */
@Entity
@Table(name = "plano_devocional")
public class PlanoDevocional {

    public enum Tipo {
        LIVRO,
        TEMA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "casa_id", nullable = false, unique = true)
    private UUID casaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tipo tipo;

    /** Código do livro (ex.: PSA) quando o tipo é LIVRO; o tema escrito quando é TEMA. */
    @Column(nullable = false)
    private String valor;

    @Column(name = "proximo_capitulo", nullable = false)
    private int proximoCapitulo = 1;

    protected PlanoDevocional() {
    }

    public PlanoDevocional(UUID casaId, Tipo tipo, String valor) {
        this.casaId = casaId;
        this.tipo = tipo;
        this.valor = valor;
    }

    public void trocar(Tipo tipo, String valor) {
        this.tipo = tipo;
        this.valor = valor;
        this.proximoCapitulo = 1;
    }

    /** Passa para o capítulo seguinte; depois do último, volta ao primeiro. */
    public void avancar(int capitulosDoLivro) {
        proximoCapitulo = proximoCapitulo >= capitulosDoLivro ? 1 : proximoCapitulo + 1;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public String getValor() {
        return valor;
    }

    public int getProximoCapitulo() {
        return proximoCapitulo;
    }
}
