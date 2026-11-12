package com.appfamilia.core.membro;

import com.appfamilia.core.casa.Casa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.util.UUID;

/** Adulto com login, sempre ligado a uma Casa. */
@Entity
public class Membro {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "casa_id")
    private Casa casa;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    /** Nulo quando a conta foi criada só com Google. */
    @Column(name = "senha_hash")
    private String senhaHash;

    /** Identificador estável da conta Google ("sub"), se vinculada. */
    @Column(name = "google_sub", unique = true)
    private String googleSub;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Papel papel;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected Membro() {
    }

    public Membro(Casa casa, String nome, String email, Papel papel) {
        this.casa = casa;
        this.nome = nome;
        this.email = email;
        this.papel = papel;
    }

    public UUID getId() {
        return id;
    }

    public Casa getCasa() {
        return casa;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public void setGoogleSub(String googleSub) {
        this.googleSub = googleSub;
    }

    public Papel getPapel() {
        return papel;
    }
}
