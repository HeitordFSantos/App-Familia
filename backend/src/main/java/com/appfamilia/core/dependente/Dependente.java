package com.appfamilia.core.dependente;

import com.appfamilia.core.casa.Casa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Filho(a), sem login. A data de nascimento é a base da agenda vacinal. */
@Entity
public class Dependente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "casa_id")
    private Casa casa;

    @Column(nullable = false)
    private String nome;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected Dependente() {
    }

    public Dependente(Casa casa, String nome, LocalDate dataNascimento) {
        this.casa = casa;
        this.nome = nome;
        this.dataNascimento = dataNascimento;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }
}
