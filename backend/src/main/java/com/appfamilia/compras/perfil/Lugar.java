package com.appfamilia.compras.perfil;

import java.text.Normalizer;
import java.util.Locale;

/** Onde o produto costuma ficar. Define quais faltas fazem sentido apontar em cada foto. */
public enum Lugar {
    GELADEIRA,
    FREEZER,
    ARMARIO,
    LIMPEZA,
    HIGIENE,
    /** Foto que não é de nenhum desses lugares: só vale o que aparece acabando. */
    OUTRO;

    /** Interpreta a resposta do modelo ("geladeira", "despensa", "área de serviço"…). */
    public static Lugar deTexto(String texto) {
        if (texto == null) {
            return OUTRO;
        }
        String t = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        if (t.contains("freezer") || t.contains("congelador")) {
            return FREEZER;
        }
        if (t.contains("geladeira") || t.contains("refrigerador")) {
            return GELADEIRA;
        }
        if (t.contains("armario") || t.contains("despensa")) {
            return ARMARIO;
        }
        if (t.contains("limpeza") || t.contains("lavanderia") || t.contains("servico")) {
            return LIMPEZA;
        }
        if (t.contains("higiene") || t.contains("banheiro")) {
            return HIGIENE;
        }
        return OUTRO;
    }
}
