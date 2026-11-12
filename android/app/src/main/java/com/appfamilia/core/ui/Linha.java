package com.appfamilia.core.ui;

/** Uma linha das listas do app: título, detalhe e, opcionalmente, uma caixa de marcação. */
public class Linha {

    public final String titulo;
    public final String detalhe;
    /** Nulo = linha sem caixa de marcação. */
    public final Boolean marcado;
    /** O objeto da API que esta linha representa. */
    public final Object dado;

    public Linha(String titulo, String detalhe, Boolean marcado, Object dado) {
        this.titulo = titulo;
        this.detalhe = detalhe;
        this.marcado = marcado;
        this.dado = dado;
    }
}
