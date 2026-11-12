package com.appfamilia.shared.ia;

import java.util.List;

/** Busca na internet. O modelo de linguagem não pesquisa sozinho: o backend busca e entrega os resultados. */
public interface Pesquisa {

    record Resultado(String titulo, String url, String trecho) {
    }

    /** Devolve até max resultados; lista vazia se a pesquisa não está configurada ou falhou. */
    List<Resultado> buscar(String consulta, int max);
}
