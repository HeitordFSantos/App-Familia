package com.appfamilia.compras.perfil;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** O histórico de um produto em uma Casa. A pontuação sai destes contadores. */
@Entity
@Table(name = "produto_casa")
public class ProdutoCasa {

    /** O que aconteceu com o produto. Cada evento tem um peso na pontuação. */
    public enum Evento {
        /** Apareceu numa foto: a família costuma ter. */
        VISTO,
        /** Entrou na lista de compras. */
        ADICIONADO,
        /** Foi marcado como comprado. */
        COMPRADO,
        /** Foi sugerido e o usuário desmarcou. */
        RECUSADO
    }

    static final int PESO_VISTO = 1;
    static final int PESO_ADICIONADO = 2;
    static final int PESO_COMPRADO = 3;
    static final int PESO_RECUSADO = -3;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "casa_id", nullable = false)
    private UUID casaId;

    @Column(nullable = false)
    private String chave;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    private Lugar lugar;

    @Column(name = "vezes_visto", nullable = false)
    private int vezesVisto;

    @Column(name = "vezes_adicionado", nullable = false)
    private int vezesAdicionado;

    @Column(name = "vezes_comprado", nullable = false)
    private int vezesComprado;

    @Column(name = "vezes_recusado", nullable = false)
    private int vezesRecusado;

    @Column(nullable = false)
    private boolean ignorado;

    @Column(name = "ultima_vez", nullable = false)
    private Instant ultimaVez = Instant.now();

    protected ProdutoCasa() {
    }

    public ProdutoCasa(UUID casaId, String chave, String nome, Lugar lugar) {
        this.casaId = casaId;
        this.chave = chave;
        this.nome = nome;
        this.lugar = lugar;
    }

    public void registrar(Evento evento) {
        switch (evento) {
            case VISTO -> vezesVisto++;
            case ADICIONADO -> vezesAdicionado++;
            case COMPRADO -> vezesComprado++;
            case RECUSADO -> vezesRecusado++;
        }
        ultimaVez = Instant.now();
    }

    /** Quanto o histórico da Casa indica que ela usa este produto. Pode ser negativo. */
    public int pontos() {
        return vezesVisto * PESO_VISTO + vezesAdicionado * PESO_ADICIONADO
                + vezesComprado * PESO_COMPRADO + vezesRecusado * PESO_RECUSADO;
    }

    public String getChave() {
        return chave;
    }

    public String getNome() {
        return nome;
    }

    public Lugar getLugar() {
        return lugar;
    }

    public void definirLugarSeVazio(Lugar novo) {
        if (lugar == null && novo != null && novo != Lugar.OUTRO) {
            lugar = novo;
        }
    }

    public boolean isIgnorado() {
        return ignorado;
    }

    public void setIgnorado(boolean ignorado) {
        this.ignorado = ignorado;
    }
}
