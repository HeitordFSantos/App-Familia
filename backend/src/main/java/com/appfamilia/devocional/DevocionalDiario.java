package com.appfamilia.devocional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** O devocional de um dia para uma Casa. É gerado uma vez e todos da Casa veem o mesmo. */
@Entity
@Table(name = "devocional_diario")
public class DevocionalDiario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "casa_id", nullable = false)
    private UUID casaId;

    @Column(name = "data_dia", nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private String referencia;

    @Column(name = "texto_biblico")
    private String textoBiblico;

    private String direitos;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String reflexao;

    private String aplicacao;

    private String oracao;

    @Column(name = "fontes_json")
    private String fontesJson;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected DevocionalDiario() {
    }

    public DevocionalDiario(UUID casaId, LocalDate data, String referencia, String textoBiblico, String direitos,
                            String titulo, String reflexao, String aplicacao, String oracao, String fontesJson) {
        this.casaId = casaId;
        this.data = data;
        this.referencia = referencia;
        this.textoBiblico = textoBiblico;
        this.direitos = direitos;
        this.titulo = titulo;
        this.reflexao = reflexao;
        this.aplicacao = aplicacao;
        this.oracao = oracao;
        this.fontesJson = fontesJson;
    }

    public LocalDate getData() {
        return data;
    }

    public String getReferencia() {
        return referencia;
    }

    public String getTextoBiblico() {
        return textoBiblico;
    }

    public String getDireitos() {
        return direitos;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getReflexao() {
        return reflexao;
    }

    public String getAplicacao() {
        return aplicacao;
    }

    public String getOracao() {
        return oracao;
    }

    public String getFontesJson() {
        return fontesJson;
    }
}
