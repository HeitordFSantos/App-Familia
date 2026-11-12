package com.appfamilia.devocional.biblia;

import java.util.Optional;

/**
 * De onde vem o texto bíblico. O texto nunca é escrito pelo modelo de linguagem, que erra
 * citações: o modelo escolhe a referência e esta fonte entrega o texto exato da tradução licenciada.
 */
public interface FonteBiblica {

    /** direitos é o aviso de direitos autorais que deve aparecer junto do texto. */
    record Texto(String conteudo, String direitos) {
    }

    /** Vazio quando a fonte não está configurada ou não tem a passagem. */
    Optional<Texto> buscar(Passagem passagem);
}
