package com.appfamilia.compras;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Item da lista de compras compartilhada da Casa. */
@Entity
@Table(name = "item_compra")
public class ItemCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "casa_id", nullable = false)
    private UUID casaId;

    @Column(nullable = false)
    private String nome;

    private String quantidade;

    @Column(nullable = false)
    private boolean comprado;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected ItemCompra() {
    }

    public ItemCompra(UUID casaId, String nome, String quantidade) {
        this.casaId = casaId;
        this.nome = nome;
        this.quantidade = quantidade;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getQuantidade() {
        return quantidade;
    }

    public boolean isComprado() {
        return comprado;
    }

    public void setComprado(boolean comprado) {
        this.comprado = comprado;
    }
}
