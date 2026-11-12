package com.appfamilia.compras.perfil;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Produto do catálogo base, carregado pela migração. Só leitura. */
@Entity
@Table(name = "produto_comum")
public class ProdutoComum {

    @Id
    private String chave;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Lugar lugar;

    @Column(nullable = false)
    private boolean basico;

    protected ProdutoComum() {
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

    public boolean isBasico() {
        return basico;
    }
}
