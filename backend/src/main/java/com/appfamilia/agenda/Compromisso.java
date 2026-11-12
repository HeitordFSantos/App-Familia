package com.appfamilia.agenda;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/** Compromisso da Casa. Pode estar ligado a um filho (consulta, escola) ou ser da família toda. */
@Entity
public class Compromisso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "casa_id", nullable = false)
    private UUID casaId;

    @Column(nullable = false)
    private String titulo;

    private String descricao;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(name = "lembrete_minutos")
    private Integer lembreteMinutos;

    @Column(name = "dependente_id")
    private UUID dependenteId;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected Compromisso() {
    }

    public Compromisso(UUID casaId, String titulo, String descricao, LocalDateTime inicio,
                       Integer lembreteMinutos, UUID dependenteId) {
        this.casaId = casaId;
        this.titulo = titulo;
        this.descricao = descricao;
        this.inicio = inicio;
        this.lembreteMinutos = lembreteMinutos;
        this.dependenteId = dependenteId;
    }

    public UUID getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public Integer getLembreteMinutos() {
        return lembreteMinutos;
    }

    public UUID getDependenteId() {
        return dependenteId;
    }
}
