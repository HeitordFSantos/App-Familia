package com.appfamilia.financas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Receita ou despesa da Casa. O valor fica em centavos para não haver erro de arredondamento. */
@Entity
public class Lancamento {

    public enum Tipo {
        RECEITA,
        DESPESA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "casa_id", nullable = false)
    private UUID casaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tipo tipo;

    @Column(name = "valor_centavos", nullable = false)
    private long valorCentavos;

    @Column(nullable = false)
    private String categoria;

    private String descricao;

    @Column(name = "data_lancamento", nullable = false)
    private LocalDate data;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected Lancamento() {
    }

    public Lancamento(UUID casaId, Tipo tipo, long valorCentavos, String categoria, String descricao, LocalDate data) {
        this.casaId = casaId;
        this.tipo = tipo;
        this.valorCentavos = valorCentavos;
        this.categoria = categoria;
        this.descricao = descricao;
        this.data = data;
    }

    public UUID getId() {
        return id;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public long getValorCentavos() {
        return valorCentavos;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getDescricao() {
        return descricao;
    }

    public LocalDate getData() {
        return data;
    }
}
