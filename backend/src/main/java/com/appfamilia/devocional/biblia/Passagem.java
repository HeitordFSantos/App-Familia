package com.appfamilia.devocional.biblia;

import com.appfamilia.devocional.biblia.Livros.Livro;

/** Um capítulo inteiro (versículos nulos) ou um intervalo de versículos dentro de um capítulo. */
public record Passagem(Livro livro, int capitulo, Integer versiculoInicio, Integer versiculoFim) {

    public static Passagem capitulo(Livro livro, int capitulo) {
        return new Passagem(livro, capitulo, null, null);
    }

    public boolean capituloInteiro() {
        return versiculoInicio == null;
    }

    /** Ex.: "Salmos 23" ou "João 3:16-18". */
    public String referencia() {
        String base = livro.nome() + " " + capitulo;
        if (capituloInteiro()) {
            return base;
        }
        if (versiculoFim == null || versiculoFim.equals(versiculoInicio)) {
            return base + ":" + versiculoInicio;
        }
        return base + ":" + versiculoInicio + "-" + versiculoFim;
    }
}
