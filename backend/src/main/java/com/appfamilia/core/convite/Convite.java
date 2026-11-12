package com.appfamilia.core.convite;

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
import java.util.UUID;

/** Convite para um segundo adulto entrar em uma Casa já existente. */
@Entity
public class Convite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "casa_id")
    private Casa casa;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "usado_em")
    private Instant usadoEm;

    protected Convite() {
    }

    public Convite(Casa casa, String email, String token, Instant expiraEm) {
        this.casa = casa;
        this.email = email;
        this.token = token;
        this.expiraEm = expiraEm;
    }

    public boolean valido() {
        return usadoEm == null && expiraEm.isAfter(Instant.now());
    }

    public void marcarUsado() {
        this.usadoEm = Instant.now();
    }

    public Casa getCasa() {
        return casa;
    }

    public String getEmail() {
        return email;
    }

    public String getToken() {
        return token;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }
}
