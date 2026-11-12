package com.appfamilia.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Conversões de data e dinheiro entre o formato da API e o que aparece na tela. */
public final class Formatos {

    private static final Locale BRASIL = new Locale("pt", "BR");

    private Formatos() {
    }

    /** "2024-03-10" vira "10/03/2024". */
    public static String dataBr(String iso) {
        if (iso == null) {
            return "";
        }
        String[] p = iso.split("-");
        return p.length == 3 ? p[2] + "/" + p[1] + "/" + p[0] : iso;
    }

    /** Data de hoje no formato AAAA-MM-DD. */
    public static String hojeIso() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    /** Converte a data devolvida pelo seletor do Material (em UTC) para AAAA-MM-DD. */
    public static String isoDoSeletor(long millisUtc) {
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        iso.setTimeZone(TimeZone.getTimeZone("UTC"));
        return iso.format(new Date(millisUtc));
    }

    /** "2026-11-12T14:30:00" vira o instante correspondente no fuso do aparelho; 0 se inválido. */
    public static long millis(String dataHoraIso) {
        if (dataHoraIso == null) {
            return 0;
        }
        String texto = dataHoraIso.length() == 16 ? dataHoraIso + ":00" : dataHoraIso;
        try {
            Date data = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(texto);
            return data == null ? 0 : data.getTime();
        } catch (Exception e) {
            return 0;
        }
    }

    /** "2026-11-12T14:30:00" vira "12/11/2026 14:30". */
    public static String dataHoraBr(String dataHoraIso) {
        long millis = millis(dataHoraIso);
        if (millis == 0) {
            return dataHoraIso == null ? "" : dataHoraIso;
        }
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", BRASIL).format(new Date(millis));
    }

    /** 123456 vira "R$ 1.234,56". */
    public static String dinheiro(long centavos) {
        return NumberFormat.getCurrencyInstance(BRASIL).format(centavos / 100.0);
    }

    /** "1.234,56", "1234,56" ou "1234.56" viram 123456. Devolve -1 se o texto não for um valor. */
    public static long centavos(String texto) {
        String limpo = texto.replace("R$", "").replace(" ", "").trim();
        if (limpo.contains(",")) {
            limpo = limpo.replace(".", "").replace(",", ".");
        }
        try {
            return new BigDecimal(limpo).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (Exception e) {
            return -1;
        }
    }
}
