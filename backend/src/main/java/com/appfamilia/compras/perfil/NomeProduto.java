package com.appfamilia.compras.perfil;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Regra única para comparar nomes de produto: "Feijões", "feijão" e "FEIJAO" viram "feijao".
 * A migração V4 usa a mesma regra para as chaves do catálogo; o teste confere as duas.
 */
public final class NomeProduto {

    public static final int TAMANHO_MAXIMO = 160;

    private NomeProduto() {
    }

    /** Sem acento, minúsculo, só letras e números, plural simples tirado. */
    public static String chave(String nome) {
        if (nome == null) {
            return "";
        }
        String s = Normalizer.normalize(nome, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        if (s.isEmpty()) {
            return "";
        }
        StringBuilder chave = new StringBuilder();
        for (String palavra : s.split(" ")) {
            if (palavra.endsWith("oes") || palavra.endsWith("aes")) {
                palavra = palavra.substring(0, palavra.length() - 3) + "ao";
            } else if (palavra.length() > 3 && palavra.endsWith("s") && !palavra.endsWith("ss")) {
                palavra = palavra.substring(0, palavra.length() - 1);
            }
            if (chave.length() > 0) {
                chave.append(' ');
            }
            chave.append(palavra);
        }
        return chave.length() > TAMANHO_MAXIMO ? chave.substring(0, TAMANHO_MAXIMO) : chave.toString();
    }

    /** Nome para mostrar: espaços arrumados e primeira letra maiúscula. */
    public static String exibicao(String nome) {
        String limpo = nome == null ? "" : nome.trim().replaceAll("\\s+", " ");
        if (limpo.length() > TAMANHO_MAXIMO) {
            limpo = limpo.substring(0, TAMANHO_MAXIMO);
        }
        return limpo.isEmpty() ? limpo : Character.toUpperCase(limpo.charAt(0)) + limpo.substring(1);
    }
}
